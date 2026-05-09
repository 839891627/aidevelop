# 07 - RAG 进阶：查询优化、混合检索与智能管道

## 1. 为什么需要进阶 RAG

基础向量检索存在以下局限：

- **关键词匹配弱：** 用户搜 "M1 阶段"，向量检索可能返回与 "第一阶段" 相关的内容，而不是包含精确字符串 "M1" 的文档。
- **短查询语义模糊：** 三个字的查询生成的向量信息量太少，相似度计算不够准确。
- **多义词问题：** "黑名单"在不同上下文中含义不同。
- **上下文缺失：** 多轮对话中 "它支持提前还款吗" 的 "它" 无法被检索系统理解。

解决思路：查询预处理 + 多路召回 + 精排，形成完整的 RAG 管道。

## 2. 查询预处理

### 2.1 查询扩展 (QueryExpansionService)

**目的：** 用同义词和专业术语扩展原始查询，提升召回率。

**实现方式：** 基于静态字典的扩展，不调用 LLM，速度极快（毫秒级）。

**字典结构：** 两套字典互补：

- `SYNONYMS`：口语词 -> 正式术语列表（如 "黑名单" -> ["征信黑名单", "不良记录", "失信名单", "失信被执行人"]）
- `TECHNICAL_TERMS`：专业术语 -> 口语表达（如 "M1" -> ["第一阶段", "1-30天"]，"征信上报" -> ["黑名单", "不良记录"]）

**扩展逻辑：**

```
原始查询: "黑名单客户怎么处理"
  ↓ 匹配 SYNONYMS["黑名单"]
扩展后: "黑名单客户怎么处理 OR 征信黑名单 OR 不良记录 OR 征信不良 OR 失信名单 OR 失信被执行人"
```

关键设计点：

- 使用 `LinkedHashSet` 保持插入顺序，原始查询始终排在最前面
- 使用 `OR` 连接所有扩展词，在向量检索中覆盖更多语义空间
- 逆向字典 `TECHNICAL_TERMS` 处理用户输入专业术语时也能匹配到口语化的文档内容

**调试接口：** `QueryExpansionService.getExpansionDetail(query)` 返回匹配了哪些同义词和专业术语。

### 2.2 查询重写 (QueryRewriteService)

**目的：** 解决多轮对话中的指代消解和上下文缺失问题。

**两阶段过滤机制：**

**第一阶段 -- 规则检查（快速，本地判断）：** `needsRewrite()` 方法检测以下模式：

- 包含代词：它、这个、那个、它们、这些、那些
- 省略主语：以 "那"、"那么"、"然后"、"接着" 开头
- 过于简短：少于 5 个字且不含问号
- 需要上下文：包含 "怎么办"、"怎么样"、"如何"

如果都不命中，直接返回原始查询，避免不必要的 LLM 调用。

**第二阶段 -- LLM 重写（慢速，远程调用）：** 将对话历史和当前问题发送给 LLM，Prompt 要求 LLM 执行四项任务：

1. 指代消解：将代词替换为具体实体
2. 补全省略：根据上下文补充完整的问题
3. 口语规范化：口语表达转书面语
4. 保持原意：不改变用户原本想问的问题

LLM 只返回重写后的查询文本，不做任何解释。

**优雅降级：** LLM 调用失败时（网络异常、超时等），返回原始查询，不影响主流程。

**调试接口：**

```
GET /api/chat/query-rewrite?query=它支持提前还款吗&conversationId=xxx
```

返回 `RewriteDetail`，包含原始查询、重写结果、是否改变、改变原因。

## 3. 多路召回

### 3.1 BM25 关键词检索 (BM25Service)

BM25 是基于 TF-IDF 改进的经典信息检索算法，本项目的实现要点：

**算法参数：**

- `k1 = 1.2`：控制词频饱和度，值越大对高频词越敏感
- `b = 0.75`：控制文档长度归一化，值越大对长文档的惩罚越强

**中文分词策略：** 采用 n-gram 方式，无需引入 jieba 等外部分词库：

- 对中文文本生成 bigram（相邻两字组合）和 trigram（相邻三字组合）
- 同时保留单字作为最细粒度匹配
- 英文和数字按空格/标点分割后转小写

**为什么需要 BM25：** 向量检索对精确关键词匹配较弱。例如查询 "CUST001"，向量检索可能找不到包含精确编号 "CUST001" 的文档，而 BM25 能精确匹配。两者互补。

**索引构建：** 在服务初始化时，通过 `vectorStore.similaritySearch` 获取所有文档，构建词频统计和文档频率统计。

### 3.2 混合检索 (HybridSearchService)

**RRF (Reciprocal Rank Fusion) 算法** 融合向量检索和 BM25 的结果：

```
final_score(doc) = sum( 1 / (k + rank_i(doc)) )  for each retrieval method
```

其中 `k = 60` 是平滑参数，防止排名靠前的文档分数过大。

**算法步骤：**

1. 分别执行向量检索和 BM25 检索，各自返回 Top-K 结果
2. 为每种检索结果建立排名（1-based）
3. 对每个文档计算 RRF 分数：`1 / (60 + rank)`
4. 同一文档在两种检索中都出现时，RRF 分数累加
5. 按最终分数降序排序，返回 Top-K

**优势：**

- 不需要归一化不同检索方法的分数（向量相似度 vs BM25 分数量级完全不同）
- 对异常值不敏感
- 实现简单，效果好

**API：**

```
GET /api/rag/hybrid-search?query=xxx&topK=5
```

返回结果包含每个文档的 RRF 分数、向量排名和 BM25 排名，便于调试分析。

## 4. 精排

### 4.1 LLM 重排序 (RerankService)

采用 **Retrieve-then-Rerank** 模式：

**流程：**

```
向量检索(低阈值, Top-20)
        ↓ 召回候选文档
所有候选文档 + query
        ↓ 发送给 LLM
LLM 对每个文档打分(0.0-1.0)
        ↓ 按新分数排序
返回 Top-K
```

**粗筛阶段：** 向量检索使用 `similarityThreshold = 0.0`（无阈值过滤），召回 `RERANK_TOP_N = 20` 个候选文档，尽可能多召回。

**LLM 打分：** 将所有候选文档内容截取前 200 字符，连同查询一起发送给 LLM。Prompt 要求 LLM 按以下标准打分：

- 1.0：完全相关，直接回答了查询问题
- 0.7-0.9：高度相关
- 0.4-0.6：部分相关
- 0.1-0.3：弱相关
- 0.0：不相关

LLM 返回格式为 `[文档编号] 分数`，如 `[1] 0.85`，通过正则解析。

**优雅降级：** LLM 调用失败或返回格式无法解析时，回退到向量检索的原始分数排序，返回结果中 `reranked = false` 标记。

**API：**

```
GET /api/rag/rerank-search?query=xxx&topK=5
```

## 5. 智能 RAG 管道 (RagPipelineService)

自动组合以上所有技术，形成 4 阶段管道：

```
阶段1: 查询重写 (QueryRewriteService)
  ↓
阶段2: 查询扩展 (QueryExpansionService)
  ↓
阶段3: 策略选择 (自动/固定)
  ↓
阶段4: 执行检索 (VectorStore / HybridSearch / Rerank)
```

### 5.1 策略选择逻辑

管道支持 4 种检索策略：

| 策略枚举 | 含义 | 触发条件 |
|---------|------|---------|
| `VECTOR_ONLY` | 纯向量检索 | 默认策略 |
| `VECTOR_WITH_RERANK` | 向量 + LLM 重排 | 问题型查询（含"如何"、"什么"等） |
| `HYBRID_SEARCH` | 向量 + BM25 混合 | 包含大写字母、数字等专有名词 |
| `HYBRID_WITH_RERANK` | 混合 + 重排 | 长查询（超过 `complexQueryLength` 阈值） |

**自动模式：** `autoMode = true` 时，根据查询特征自动选择策略。特征检测包括：

- `isQuestionQuery()`：查询包含配置的问题词（"怎么办"、"如何"、"怎么"、"为什么"等）
- `hasTechnicalTerms()`：包含大写字母、数字、或大写+数字组合
- `isLongQuery()`：查询长度超过 `complexQueryLength`（默认 10 字）

**固定模式：** `autoMode = false` 时，根据 `enableHybridSearch` 和 `enableRerank` 的开关组合确定策略。

### 5.2 配置开关

`RagPipelineProperties` 对应 `application.yml` 的 `app.chat.rag.pipeline` 段：

```yaml
app:
  chat:
    rag:
      pipeline:
        enable-query-rewrite: true      # 启用查询重写
        enable-query-expansion: true    # 启用查询扩展
        enable-hybrid-search: false     # 启用混合检索（默认关闭，需更多计算）
        enable-rerank: false            # 启用重排序（默认关闭，速度慢）
        auto-mode: true                 # 自动选择策略
        complex-query-length: 10        # 复杂查询长度阈值
        question-words:                 # 问题词列表
          - 怎么办
          - 如何
          - 怎么
          - 为什么
          - 是什么
          - 哪些
          - 如何处理
```

每个阶段可通过开关独立控制，关闭的阶段直接跳过。

### 5.3 管道执行结果

`PipelineResult` 包含完整的信息链路：

- `originalQuery`：用户原始输入
- `rewrittenQuery`：重写后的查询
- `expandedQuery`：扩展后的查询
- `strategy`：选择的检索策略枚举
- `documents`：最终检索到的文档列表

可通过 `getTransformationSummary()` 获取查询变换的摘要文本。

**API：**

```
GET /api/rag/pipeline?query=xxx&conversationId=xxx&topK=5
```

## 6. RAG 评估 (RagEvaluationService)

### 6.1 为什么需要评估

没有量化指标就无法判断优化方向。评估回答两个问题：找得全不全（召回率）？找得准不准（精确率）？

### 6.2 五个核心指标

| 指标 | 公式 | 含义 |
|------|------|------|
| **Recall（召回率）** | 检索到的相关文档数 / 所有相关文档数 | 应该找到的文档，找到了多少 |
| **Precision（精确率）** | 检索到的相关文档数 / 检索到的总文档数 | 找到的文档中，有多少是真正相关的 |
| **F1** | 2 * P * R / (P + R) | 召回率和精确率的调和平均，综合指标 |
| **MRR** | 1 / 第一个相关文档的排名 | 第一个正确结果排得有多靠前 |
| **NDCG** | DCG / IDCG | 考虑排序位置的指标，相关文档排在越前面分数越高 |

**使用方式：** 人工标注一批"查询 -> 相关文档ID"的测试集，调用评估接口自动计算指标。

**API：**

```
POST /api/rag/evaluate
Body: { "query": "xxx", "relevantDocIds": ["id1", "id2"], "topK": 5 }
```

支持批量评估（`batchEvaluate`），对多个查询计算平均指标。

## 7. 检索策略对比总结

| 策略 | 适用场景 | 优点 | 缺点 |
|------|---------|------|------|
| 纯向量 (VECTOR_ONLY) | 语义匹配、同义词查询 | 理解语义，实现简单 | 精确关键词匹配弱 |
| 向量+重排 (VECTOR_WITH_RERANK) | 需要精确回答的问题型查询 | 高精确率，LLM 深度理解 | 慢，需要额外 LLM 调用 |
| 混合检索 (HYBRID_SEARCH) | 通用场景，专有名词 | 覆盖全面，向量+关键词互补 | 计算量比纯向量大 |
| 混合+重排 (HYBRID_WITH_RERANK) | 复杂长查询 | 最佳效果 | 最慢，多路召回+LLM 打分 |

实际选择建议：先用纯向量跑通流程，遇到召回不足加查询扩展，遇到关键词匹配问题加混合检索，对精确率有要求加重排序。

## 8. 动手实验

1. **策略对比：** 用同一个查询（如 "M1 阶段的征信上报流程"）分别调用 `/search`、`/hybrid-search`、`/rerank-search`、`/pipeline` 接口，对比结果差异。
2. **同义词扩展：** 在 `QueryExpansionService` 的 `SYNONYMS` 字典中添加新的映射，重启后观察包含该词的查询召回率变化。
3. **量化评估：** 准备 5-10 个测试查询，标注每个查询的相关文档 ID，调用 `/evaluate` 接口，对比不同策略的 Recall 和 Precision。
4. **BM25 参数调优：** 修改 `BM25Service` 中的 `K1`（默认 1.2）和 `B`（默认 0.75），观察关键词检索效果的变化。
5. **管道开关：** 逐个开关 `enable-query-rewrite`、`enable-query-expansion`、`enable-hybrid-search`、`enable-rerank`，观察每个组件对最终结果的影响。

## 9. 关键代码文件

| 文件 | 说明 |
|------|------|
| `src/main/java/.../service/rag/QueryRewriteService.java` | 查询重写（指代消解、上下文补全） |
| `src/main/java/.../service/rag/QueryExpansionService.java` | 查询扩展（同义词、专业术语） |
| `src/main/java/.../service/rag/BM25Service.java` | BM25 关键词检索 |
| `src/main/java/.../service/rag/HybridSearchService.java` | 混合检索（RRF 融合） |
| `src/main/java/.../service/rag/RerankService.java` | LLM 重排序 |
| `src/main/java/.../service/rag/RagPipelineService.java` | 智能 RAG 管道（组合以上所有） |
| `src/main/java/.../service/rag/RagEvaluationService.java` | RAG 评估（Recall、Precision、F1、MRR、NDCG） |
| `src/main/java/.../config/RagPipelineProperties.java` | 管道配置开关 |
