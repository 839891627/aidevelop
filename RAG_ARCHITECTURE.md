# RAG 智能对话系统架构文档

## 📋 目录

- [1. 项目概述](#1-项目概述)
- [2. 系统架构](#2-系统架构)
- [3. 核心组件](#3-核心组件)
- [4. RAG 流程说明](#4-rag-流程说明)
- [5. API 接口文档](#5-api-接口文档)
- [6. 配置说明](#6-配置说明)
- [7. 部署指南](#7-部署指南)
- [8. 使用示例](#8-使用示例)
- [9. 最佳实践](#9-最佳实践)
- [10. 故障排查](#10-故障排查)

---

## 1. 项目概述

### 1.1 项目简介

**项目名称**：AI Chat Assistant
**项目类型**：Spring Boot + Spring AI 智能对话系统
**核心技术**：RAG（检索增强生成）、向量检索、大语言模型

### 1.2 业务场景

本项目是一个金融领域的智能助手系统，具备以下能力：

- 📚 **知识库问答**：基于业务规则、产品手册、风控指南等文档进行回答
- 🔍 **数据查询**：通过 Function Calling 查询借款、还款等实际业务数据
- 🧠 **多轮对话**：支持上下文理解和指代消解（"它"指的是什么）
- 🎯 **智能检索**：自动选择最佳检索策略（向量检索、混合检索、重排序）

### 1.3 技术栈

```
后端框架：Spring Boot 3.3.5
AI 框架：Spring AI 1.0.0-M5
语言：Java 17
数据库：MySQL 8.0
向量数据库：SimpleVectorStore（本地文件）
Embedding 模型：智谱 AI（用于中文向量化）
LLM：DeepSeek（通过 OpenAI 兼容接口）
文档格式：TXT、PDF
API 文档：SpringDoc OpenAPI 3.0
```

---

## 2. 系统架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                          用户层                                  │
│  Web 前端 / 移动端 / 第三方系统 (通过 REST API 调用)              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────────────┐
│                         API 层                                   │
│  ┌────────────────┐  ┌───────────────┐  ┌─────────────────┐   │
│  │ ChatController │  │ HealthController│  │ModelController │   │
│  │  聊天接口       │  │   健康检查      │  │   模型管理      │   │
│  └────────────────┘  └───────────────┘  └─────────────────┘   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────────────┐
│                         服务层                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    ChatService                           │  │
│  │  ├─ 普通聊天（阻塞式）                                     │  │
│  │  └─ 流式聊天（SSE）                                        │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              RAG 高级技巧服务（service/rag/）              │  │
│  │  ├─ QueryExpansionService    # 查询扩展                │  │
│  │  ├─ QueryRewriteService       # 查询重写                │  │
│  │  ├─ BM25Service               # BM25 关键词检索         │  │
│  │  ├─ HybridSearchService       # 混合检索                │  │
│  │  ├─ RerankService             # 重排序                  │  │
│  │  ├─ RagPipelineService        # 智能 RAG 管道           │  │
│  │  └─ RagEvaluationService      # RAG 评估               │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │            Function Calling（service/function/）              │  │
│  │  ├─ LoanQueryFunction          # 借款查询               │  │
│  │  ├─ RepaymentQueryFunction     # 还款查询               │  │
│  │  └─ RiskAssessmentFunction     # 风险评估               │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────────────┐
│                         AI 层                                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    ChatClient                             │  │
│  │  ├─ MessageChatMemoryAdvisor    # 对话记忆              │  │
│  │  ├─ QuestionAnswerAdvisor      # RAG 检索              │  │
│  │  └─ Function Calling Advisor    # 函数调用              │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌────────────────┐  ┌──────────────┐  ┌─────────────────┐    │
│  │ OpenAI LLM      │  │ Anthropic LLM│  │ ZhipuAI Embed │    │
│  │ (DeepSeek)      │  │ (Claude)     │  │ (中文向量化)   │    │
│  └────────────────┘  └───────────────┘  └─────────────────┘    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ↓
┌─────────────────────────────────────────────────────────────────┐
│                      数据层                                      │
│  ┌────────────────┐  ┌──────────────┐  ┌─────────────────┐    │
│  │ VectorStore     │  │ MySQL 数据库  │  │ 知识库文件      │    │
│  │ (向量数据库)    │  │ (业务数据)    │  │ TXT/PDF        │    │
│  └────────────────┘  └───────────────┘  └─────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 RAG 系统架构图

```
┌──────────────────────────────────────────────────────────────┐
│                        用户查询                                │
│                  "客户逾期了怎么办？"                             │
└───────────────────────┬──────────────────────────────────────┘
                        ↓
        ┌───────────────┴───────────────┐
        │   RAG 智能 Pipeline          │
        └───────────────┬───────────────┘
                        ↓
    ┌───────────────────────────────────┐
    │  阶段 1: 查询重写（多轮对话）      │
    │  "它怎么办" → "产品A逾期怎么办"    │
    │  ✓ 启用条件：有对话历史             │
    └───────────────┬───────────────────┘
                    ↓
    ┌───────────────────────────────────┐
    │  阶段 2: 查询扩展（同义词）        │
    │  "逾期" → "逾期 OR 拖欠 OR 违约"   │
    │  ✓ 启用条件：总是启用               │
    └───────────────┬───────────────────┘
                    ↓
    ┌───────────────────────────────────┐
    │  阶段 3: 智能策略选择             │
    │                                       │
    │  简单查询 → 向量检索                │
    │  专有名词 → 混合检索（向量+BM25） │
    │  问题型查询 → 向量检索+重排序      │
    │  复杂查询 → 混合检索+重排序        │
    └───────────────┬───────────────────┘
                    ↓
    ┌───────────────────────────────────┐
    │  阶段 4: 检索执行                  │
    │  - 向量检索（语义理解）            │
    │  - BM25检索（关键词匹配）         │
    │  - LLM重排序（精确排序）          │
    └───────────────┬───────────────────┘
                    ↓
    ┌───────────────────────────────────┐
    │  阶段 5: 结果返回                  │
    │  - Top-K 文档片段                   │
    │  - 包含元数据、分数                  │
    └───────────────────────────────────┘
```

### 2.3 数据流向图

```
知识库文档（TXT/PDF）
    ↓
文档加载与切分（TokenTextSplitter）
    ↓
向量化（ZhipuAI Embedding）
    ↓
存入向量库（SimpleVectorStore）
    ↓
    ↓
用户查询
    ↓
查询扩展（同义词、专业术语）
    ↓
向量检索 / 混合检索
    ↓
召回 Top-K 候选文档
    ↓
重排序（可选，LLM 打分）
    ↓
拼接为 Prompt（Document + Context）
    ↓
LLM 生成回答（DeepSeek）
    ↓
返回给用户
```

---

## 3. 核心组件

### 3.1 组件列表

| 组件 | 功能 | 输入 | 输出 |
|------|------|------|------|
| **ChatService** | 聊天服务 | 用户消息、对话ID | AI 回答 |
| **QueryExpansionService** | 查询扩展 | 原始查询 | 扩展查询（OR连接） |
| **QueryRewriteService** | 查询重写 | 原始查询、对话历史 | 重写后的查询 |
| **BM25Service** | BM25 关键词检索 | 查询、TopK | BM25 检索结果 |
| **HybridSearchService** | 混合检索 | 查询、TopK | 融合检索结果 |
| **RerankService** | 重排序 | 查询、TopK | 重排序后的结果 |
| **RagPipelineService** | 智能 RAG 管道 | 查询、对话ID | 最优检索结果 |
| **RagEvaluationService** | RAG 评估 | 查询、相关文档 | 评估指标 |

### 3.2 组件详细说明

#### 3.2.1 QueryExpansionService（查询扩展）

**功能**：通过同义词和专业术语扩展，提升检索召回率

**核心逻辑**：
```
输入："黑名单"
↓
匹配同义词词典
↓
输出："黑名单 OR 征信黑名单 OR 不良记录 OR 征信不良 OR 失信名单 OR 失信被执行人"
```

**词典示例**：
```java
// 同义词映射
"黑名单" → ["征信黑名单", "不良记录", "征信不良", "失信名单"]
"逾期" → ["OVERDUE", "拖欠", "欠款", "违约"]

// 专业术语映射
"M1" → ["第一阶段", "初期阶段", "1-30天"]
"M2" → ["第二阶段", "中期阶段", "31-90天"]
```

#### 3.2.2 QueryRewriteService（查询重写）

**功能**：基于对话历史重写查询，实现指代消解和上下文补全

**核心逻辑**：
```
对话历史：
  用户："产品A的利率是多少？"
  AI："产品A的年化利率是12%。"

当前查询："它支持提前还款吗？"
    ↓
LLM 重写 → "产品A是否支持提前还款？"
```

**重写规则**：
- 指代消解：将"它"、"这个"替换为具体实体
- 省略主语：根据上下文补全省略内容
- 口语化规范化：转换为规范表达

#### 3.2.3 BM25Service（BM25 关键词检索）

**功能**：基于关键词匹配的检索，擅长专有名词、代码、数字精确匹配

**BM25 公式**：
```
score(D,Q) = Σ IDF(qi) × (f(qi,D) × (k1 + 1)) / (f(qi,D) + k1 × (1 - b + b × |D| / avgdl))

参数：
- k1 = 1.2（词频饱和度）
- b = 0.75（长度归一化）
```

**优势**：
- 精确匹配专有名词（M1、CUST001）
- 精确匹配数字（30天、90天）
- 与向量检索互补

#### 3.2.4 HybridSearchService（混合检索）

**功能**：结合向量检索和 BM25 检索的优势

**融合算法（RRF）**：
```java
score = 1/(60 + vector_rank) + 1/(60 + bm25_rank)
```

**对比**：
| 特性 | 向量检索 | BM25 | 混合检索 |
|------|---------|------|----------|
| 语义理解 | ✅ | ❌ | ✅ |
| 关键词精确 | ❌ | ✅ | ✅ |
| 检索速度 | 快 | 快 | 稍慢 |

#### 3.2.5 RerankService（重排序）

**功能**：使用 LLM 对召回的文档进行精确重排序

**流程**：
```
1. 向量检索召回 Top-20（粗筛）
2. LLM 分析每个文档与查询的相关性（0-1分）
3. 按新分数排序，返回 Top-5（精排）
```

**LLM 评分规则**：
- 1.0：完全相关，直接回答问题
- 0.7-0.9：高度相关，包含有用信息
- 0.4-0.6：部分相关
- 0.0：不相关

#### 3.2.6 RagPipelineService（智能 RAG 管道）

**功能**：根据查询特点自动选择最佳检索策略

**策略选择逻辑**：
```
if (问题型查询 && 启用重排序) {
    return 向量检索 + 重排序;
}
if (专有名词 && 启用混合检索) {
    return 混合检索;
}
if (复杂查询 && 全部启用) {
    return 混合检索 + 重排序;
}
return 向量检索;  // 默认策略
```

---

## 4. RAG 流程说明

### 4.1 标准检索流程

```
用户查询 → 向量库检索 → 返回 Top-K 文档 → 拼接为 Prompt → LLM 生成回答
```

### 4.2 增强检索流程（本系统）

```
用户查询
    ↓
┌─────────────────────┐
│ 查询重写（可选）    │
│ 多轮对话时启用       │
└─────────┬───────────┘
          ↓
┌─────────────────────┐
│ 查询扩展            │
│ 添加同义词、专业术语  │
└─────────┬───────────┘
          ↓
┌─────────────────────┐
│ 智能检索策略选择     │
│ - 向量检索           │
│ - 混合检索           │
│ - 重排序             │
└─────────┬───────────┘
          ↓
    返回 Top-K 文档
```

### 4.3 RAG 评估流程

```
准备评估数据集
（查询 + 相关文档标注）
    ↓
执行检索
    ↓
计算评估指标
- 召回率：相关文档被检索到的比例
- 精确率：检索到的文档中相关的比例
- F1分数：综合指标
- MRR：第一个相关文档的平均排名
- NDCG：考虑排序位置的综合指标
    ↓
对比目标指标
    ↓
优化系统参数
- 调整相似度阈值
- 优化查询扩展词典
- 启用/禁用特定技巧
```

---

## 5. API 接口文档

### 5.1 接口总览

| 接口路径 | 方法 | 功能 | 使用场景 |
|---------|------|------|----------|
| `/api/chat` | POST | 普通聊天 | 阻塞式对话 |
| `/api/chat/stream` | POST | 流式聊天 | 实时流式响应 |
| `/api/chat/{conversationId}` | DELETE | 清空对话 | 清除对话历史 |
| `/api/chat/search` | GET | 知识库检索 | 向量检索 |
| `/api/chat/hybrid-search` | GET | 混合检索 | 向量+BM25 |
| `/api/chat/rerank-search` | GET | 重排序检索 | 高精度检索 |
| **`/api/chat/pipeline`** | **GET** | **智能RAG管道** | **自动选择最佳策略** ⭐ |
| `/api/chat/query-expansion` | GET | 查询扩展调试 | 查看扩展详情 |
| `/api/chat/query-rewrite` | GET | 查询重写调试 | 查看重写详情 |
| `/api/chat/test-search` | GET | 相似度测试 | 测试检索分数 |
| `/api/chat/debug` | GET | 向量库调试 | 查看文档内容 |
| `/api/chat/evaluate` | POST | RAG系统评估 | 评估检索效果 |

### 5.2 核心接口详解

#### 5.2.1 智能RAG管道接口 ⭐

**接口**：`GET /api/chat/pipeline`

**功能**：自动组合使用多种 RAG 技巧，返回最佳检索结果

**请求参数**：
```json
{
  "query": "客户逾期了怎么办",
  "conversationId": "xxx",  // 可选，用于多轮对话
  "topK": 5
}
```

**返回示例**：
```json
{
  "originalQuery": "客户逾期了怎么办",
  "rewrittenQuery": "客户逾期了怎么办",  // 如果没有对话历史，则不变
  "expandedQuery": "客户逾期 OR 拖欠 OR 违约 OR 欠款了怎么办",
  "strategy": "VECTOR_WITH_RERANK",
  "transformationSummary": "原始查询: 客户逾期了怎么办\n  → 扩展: ...\n  → 策略: VECTOR_WITH_RERANK\n  → 结果数: 5",
  "documents": [
    {
      "content": "逾期处理流程...",
      "metadata": {"type": "规则", "filename": "rules.txt"},
      "score": 0.85
    }
  ]
}
```

#### 5.2.2 混合检索接口

**接口**：`GET /api/chat/hybrid-search`

**功能**：结合向量检索（语义理解）和 BM25（关键词匹配）

**请求参数**：
```json
{
  "query": "M1阶段的征信上报规则",
  "topK": 5
}
```

**返回示例**：
```json
[
  {
    "content": "M1阶段（1-30天）：...征信上报...",
    "metadata": {...},
    "finalScore": 0.0325,  // RRF 融合分数
    "vectorRank": 2,       // 向量检索排名
    "bm25Rank": 1,         // BM25检索排名
    "vectorScore": 0.78,   // 向量检索分数
    "bm25Score": 12.3,     // BM25分数
    "source": "BOTH"       // 检索来源标识
  }
]
```

#### 5.2.3 RAG系统评估接口 ⭐

**接口**：`POST /api/chat/evaluate`

**功能**：评估 RAG 系统的检索效果，计算五大指标

**请求参数**：
```json
{
  "query": "逾期处理流程",
  "relevantDocIds": ["doc1", "doc3", "doc5"],
  "topK": 5,
  "minTargetRecall": 0.7,
  "minTargetPrecision": 0.75
}
```

**返回示例**：
```json
{
  "query": "逾期处理流程",
  "retrievedCount": 5,
  "relevantCount": 3,
  "recall": 0.667,      // 召回率：检索到 2/3
  "precision": 0.8,     // 精确率：2/5 是相关的
  "f1": 0.727,          // F1分数
  "mrr": 0.5,            // MRR：第一个相关文档排第2
  "ndcg": 0.75,          // NDCG
  "meetsTarget": false,   // 是否达到目标
  "detailedInfo": "检索到的文档:\n  [1] ID=doc1, 相关=✓, ...\n"
}
```

---

## 6. 配置说明

### 6.1 配置文件结构

```yaml
app:
  chat:
    rag:
      # 基础检索配置
      similarity-threshold: 0.2    # 相似度阈值
      top-k: 5                     # 检索返回数量

      # RAG 管道配置
      pipeline:
        enable-query-rewrite: true     # 查询重写
        enable-query-expansion: true   # 查询扩展
        enable-hybrid-search: false    # 混合检索（默认关闭）
        enable-rerank: true            # 重排序（已开启）
        auto-mode: true                 # 自动模式

        complex-query-length: 10       # 复杂查询阈值
        question-words:                 # 问题词列表
          - 怎么办
          - 如何
          - 为什么
```

### 6.2 关键参数说明

| 参数 | 默认值 | 说明 | 优化建议 |
|------|--------|------|----------|
| `similarity-threshold` | 0.2 | 相似度阈值，低于此值的文档将被过滤 | ZhipuAI 建议 0.2，过低会召回太多不相关文档 |
| `top-k` | 5 | 检索返回的文档数量 | 简单查询 5 个，复杂查询 10 个 |
| `enable-query-rewrite` | true | 是否启用查询重写 | 多轮对话场景必须启用 |
| `enable-query-expansion` | true | 是否启用查询扩展 | 建议始终启用 |
| `enable-hybrid-search` | false | 是否启用混合检索 | 有专有名词时开启 |
| `enable-rerank` | true | 是否启用重排序 | 问题型查询开启 |
| `auto-mode` | true | 是否自动选择策略 | 建议启用，系统会自动判断 |

### 6.3 参数调优指南

#### 场景 1：召回率低（< 0.8）

**症状**：相关文档检索不到

**解决方案**：
```yaml
# 降低相似度阈值
similarity-threshold: 0.2 → 0.15

# 增加 TopK
top-k: 5 → 10

# 启用查询扩展（检查词典是否完善）
enable-query-expansion: true
```

#### 场景 2：精确率低（< 0.75）

**症状**：检索到太多不相关文档

**解决方案**：
```yaml
# 提高相似度阈值
similarity-threshold: 0.2 → 0.3

# 启用重排序
enable-rerank: true

# 减少 TopK
top-k: 5 → 3
```

#### 场景 3：专有名词检索不到

**症状**：查询 "M1阶段" 检索不到结果

**解决方案**：
```yaml
# 启用混合检索
enable-hybrid-search: true
```

#### 场景 4：问题型查询答案不准确

**症状**：问"怎么办"却返回"后果"

**解决方案**：
```yaml
# 启用重排序
enable-rerank: true
```

---

## 7. 部署指南

### 7.1 环境要求

```
- Java 17+
- Maven 3.6+
- MySQL 8.0+
- 至少 2GB 内存
- 智谱 AI API Key（用于 Embedding）
- DeepSeek API Key（用于 LLM）
```

### 7.2 部署步骤

#### 步骤 1：配置环境变量

```bash
# 设置智谱 AI API Key（用于向量化）
export ZHIPUAI_API_KEY=your_zhipuai_api_key

# 设置 DeepSeek API Key（用于 LLM，兼容 OpenAI 格式）
export OPENAI_API_KEY=your_deepseek_api_key

# 设置数据库密码（可选）
export DB_PASSWORD=your_db_password
```

#### 步骤 2：修改配置文件

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  profiles:
    active: openai  # 使用 DeepSeek

spring:
  ai:
    zhipuai:
      api-key: ${ZHIPUAIAI_API_KEY}
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.deepseek.com  # DeepSeek API 地址

  datasource:
    url: jdbc:mysql://localhost:3306/fundcore
    username: root
    password: ${DB_PASSWORD:}
```

#### 步骤 3：准备知识库文档

将知识库文档（TXT/PDF）放在 `src/main/resources/knowledge/` 目录：

```
src/main/resources/knowledge/
├── rules.txt           # 业务规则
├── product.txt        # 产品手册
├── risk.txt           # 风控指南
└── contract.txt        # 合同模板
```

#### 步骤 4：编译打包

```bash
# 使用 Java 17 编译
export JAVA_HOME=/path/to/java17

# 清理并打包
mvn clean package -DskipTests
```

#### 步骤 5：运行应用

```bash
# 直接运行 JAR
java -jar target/aidevelop-1.0.0.jar

# 或使用 Maven
mvn spring-boot:run
```

### 7.3 验证部署

```bash
# 1. 健康检查
curl http://localhost:8080/api/health

# 2. 查看向量库状态
curl http://localhost:8080/api/chat/debug

# 3. 测试检索
curl "http://localhost:8080/api/chat/search?query=逾期&topK=3"

# 4. 访问 Swagger UI
open http://localhost:8080/swagger-ui.html
```

---

## 8. 使用示例

### 8.1 简单问答

```bash
# 场景：用户询问业务规则
curl -X POST "http://localhost:8080/api/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "逾期多少天会进入黑名单？"
  }'

# 系统会：
# 1. 检索知识库中关于"逾期"和"黑名单"的规则
# 2. 返回准确答案："根据业务规则，逾期90天以上将上报征信黑名单"
```

### 8.2 多轮对话

```bash
# 第一轮
curl -X POST "http://localhost:8080/api/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "产品A的利率是多少？"
  }'
# 返回："产品A的年化利率是12%"

# 第二轮（使用 conversationId）
curl -X POST "http://localhost:8080/api/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "它支持提前还款吗？",
    "conversationId": "上一轮返回的ID"
  }'
# 系统会：
# 1. 查询重写："它" → "产品A"
# 2. 检索产品A的提前还款规则
# 3. 返回答案
```

### 8.3 数据查询

```bash
# 场景：查询具体的客户数据
curl -X POST "http://localhost:8080/api/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "客户 CUST001 的借款金额是多少？"
  }'

# 系统会：
# 1. 识别为数据查询
# 2. 调用 Function Calling
# 3. 查询数据库
# 4. 返回结果："客户 CUST001 的借款金额为 50,000 元"
```

### 8.4 复杂问题（混合规则+数据）

```bash
# 场景：风险评估
curl -X POST "http://localhost:8080/api/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "客户 CUST001 现在风险高吗？应该怎么处理？"
  }'

# 系统会：
# 1. 查询客户数据（Function Calling）
# 2. 检索风险处理规则（RAG）
# 3. 综合分析：
#    - "客户当前逾期45天"
#    - "根据规则属于M2阶段"
#    - "应该采取电话催收+限制额度"
```

---

## 9. 最佳实践

### 9.1 知识库构建

#### 文档组织

```
knowledge/
├── 01_业务规则/
│   ├── 逾期处理规则.txt
│   ├── 征信上报规则.txt
│   └── 黑名单规则.txt
├── 02_产品手册/
│   ├── 产品A介绍.txt
│   ├── 产品B介绍.txt
│   └── 费率说明.txt
├── 03_风控指南/
│   ├── M1阶段处理.txt
│   ├── M2阶段处理.txt
│   └── M3阶段处理.txt
└── 04_合同模板/
    ├── 借款合同.txt
    └── 担保合同.txt
```

#### 文档编写规范

1. **使用结构化格式**
```
## 逾期处理规则

### M1阶段（1-30天）
- 处理方式：电话提醒
- 联系频率：每3天一次
- 记录要求：留存通话记录

### M2阶段（31-90天）
- 处理方式：短信+电话
- 上报征信：是
- 限制额度：是
```

2. **明确关键词**
```
✅ 好的写法："客户逾期90天以上，将上报征信黑名单"
❌ 差的写法："超过90天会上报"（缺少"逾期"上下文）
```

3. **添加元数据标签**
```
在文档开头添加标签：
类型：规则
类别：逾期处理
产品：通用
```

### 9.2 查询优化

#### 用户提问教育

```
❌ 差："查一下"
✅ 好："逾期多少天会上征信黑名单？"

❌ 差："它利率高吗？"
✅ 好："产品A的利率是多少？"
```

#### 多轮对话技巧

```
第一轮：建立上下文
用户："产品A的利率是多少？"
系统："产品A的年化利率是12%。"

第二轮：明确指代
用户："它支持提前还款吗？"  ✓ 明确
用户："那怎么办？"                ✗ 不明确
```

### 9.3 性能优化

#### 缓存策略

```
1. 向量库缓存
   - 本地文件存储（./data/vector-store.json）
   - 重启后无需重新计算

2. 对话历史缓存
   - InMemoryChatMemory
   - 每个对话独立存储

3. 查询扩展缓存（待实现）
   - 同义词词典编译后缓存
   - 避免每次查询都解析
```

#### 批量查询优化

```
如果需要处理大量查询：

1. 批量评估接口
2. 并行处理
3. 结果缓存
```

### 9.4 监控与维护

#### 日志监控

```yaml
logging:
  level:
    root: INFO
    com.example.aidevelop: DEBUG
    org.springframework.ai: DEBUG
```

#### 关键指标监控

```java
// 建议监控的指标
- 查询响应时间（P95 < 3s）
- 检索召回率（目标 > 0.8）
- 检索精确率（目标 > 0.75）
- LLM 调用成功率（目标 > 99%）
```

#### 定期评估

```
建议每周运行一次完整评估：

1. 准备评估数据集（50-100个查询）
2. 调用评估接口
3. 分析评估结果
4. 调整系统参数
```

---

## 10. 故障排查

### 10.1 常见问题

#### 问题 1：向量库为空

**症状**：
```
GET /api/chat/debug 返回：
{
  "totalDocuments": 0
}
```

**原因**：
- 知识库文档未正确加载
- 向量存储文件损坏

**解决方案**：
```bash
# 1. 检查知识库文件
ls -la src/main/resources/knowledge/

# 2. 删除向量存储文件，触发重建
rm ./data/vector-store.json

# 3. 重启应用，会自动重建向量库
mvn spring-boot:run
```

#### 问题 2：检索结果不相关

**症状**：
```
查询："逾期处理"
返回：产品介绍、利率计算等无关内容
```

**原因**：
- 相似度阈值过高
- 知识库文档质量问题
- 查询词不匹配

**解决方案**：
```yaml
# 1. 降低相似度阈值
similarity-threshold: 0.3 → 0.2

# 2. 检查日志，查看实际检索到的文档
logging:
  level:
    com.example.aidevelop.service.rag: DEBUG

# 3. 使用测试接口分析
GET /api/chat/test-search?query=逾期
```

#### 问题 3：查询扩展不起作用

**症状**：
```
GET /api/chat/query-expansion?query=黑名单
返回：
{
  "expandedQuery": "黑名单"  # 没有扩展
}
```

**原因**：
- 词典中没有该词
- 配置未启用查询扩展

**解决方案**：
```java
// 1. 检查 QueryExpansionService.java 词典
// 2. 添加相关同义词
SYNONYMS.put("你的词", List.of("同义词1", "同义词2"));

// 3. 重新编译运行
mvn clean compile && mvn spring-boot:run
```

#### 问题 4：LLM 响应慢

**症状**：
```
查询响应时间 > 10秒
```

**原因**：
- 启用了重排序（额外的 LLM 调用）
- 检索召回数量过多（Top-K 太大）
- LLM API 网络延迟

**解决方案**：
```yaml
# 1. 减少 Top-K
top-k: 10 → 5

# 2. 禁用重排序（如果精度可接受）
enable-rerank: false

# 3. 使用流式接口提升用户体验
POST /api/chat/stream
```

#### 问题 5：多轮对话中指代消解失败

**症状**：
```
第一轮："产品A的利率是多少？"
第二轮："它支持提前还款吗？"
结果：系统不知道"它"指产品A
```

**原因**：
- conversationId 未传递
- 对话历史未正确保存

**解决方案**：
```bash
# 1. 确保第二轮请求包含 conversationId
curl -X POST "http://localhost:8080/api/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "它支持提前还款吗？",
    "conversationId": "第一轮返回的ID"  # ✓ 必须
  }'

# 2. 检查日志
# 应该看到："查询重写: 它 → 产品A"
```

### 10.2 调试技巧

#### 使用测试接口

```bash
# 1. 测试查询扩展
curl "http://localhost:8080/api/chat/query-expansion?query=黑名单"

# 2. 测试查询重写（需要conversationId）
curl "http://localhost:8080/api/chat/query-rewrite?query=它支持吗&conversationId=xxx"

# 3. 测试混合检索
curl "http://localhost:8080/api/chat/hybrid-search?query=M1阶段&topK=3"

# 4. 测试RAG管道（查看完整流程）
curl "http://localhost:8080/api/chat/pipeline?query=客户逾期了怎么办&topK=3"
```

#### 查看日志

```bash
# 实时查看日志
tail -f logs/aidevelop.log

# 搜索特定关键词
grep "查询扩展" logs/aidevelop.log
grep "策略选择" logs/aidevelop.log
grep "重排序" logs/aidevelop.log
```

#### Swagger UI 测试

```
访问：http://localhost:8080/swagger-ui.html

优势：
- 可视化接口文档
- 在线测试
- 查看请求/响应示例
```

---

## 附录

### A. 项目文件结构

```
aidevelop/
├── src/main/java/com/example/aidevelop/
│   ├── config/                    # 配置类
│   │   ├── AiModelConfig.java     # AI 模型配置
│   │   ├── RagProperties.java    # RAG 基础配置
│   │   ├── RagPipelineProperties.java  # RAG 管道配置
│   │   ├── VectorStoreConfig.java  # 向量库配置
│   │   ├── CorsConfig.java         # 跨域配置
│   │   └── SwaggerConfig.java     # API 文档配置
│   │
│   ├── controller/                # 控制器层
│   │   ├── ChatController.java   # 聊天接口
│   │   ├── HealthController.java # 健康检查
│   │   └── ModelController.java  # 模型管理
│   │
│   ├── service/                   # 服务层
│   │   ├── ChatService.java       # 聊天服务接口
│   │   ├── impl/                  # 服务实现
│   │   │   └── ChatServiceImpl.java
│   │   ├── rag/                    # RAG 服务 ⭐
│   │   │   ├── QueryExpansionService.java
│   │   │   ├── QueryRewriteService.java
│   │   │   ├── BM25Service.java
│   │   │   ├── HybridSearchService.java
│   │   │   ├── RerankService.java
│   │   │   ├── RagPipelineService.java
│   │   │   └── RagEvaluationService.java
│   │   └── function/              # Function Calling
│   │       ├── LoanQueryFunction.java
│   │       ├── RepaymentQueryFunction.java
│   │       └── RiskAssessmentFunction.java
│   │
│   ├── model/                     # 数据模型
│   │   ├── dto/                    # 数据传输对象
│   │   │   ├── chat/              # 聊天 DTO
│   │   │   │   ├── ChatRequest.java
│   │   │   │   └── ChatResponse.java
│   │   │   └── rag/               # RAG DTO ⭐
│   │   │       ├── QueryExpansionDetailDTO.java
│   │   │       ├── QueryRewriteDetailDTO.java
│   │   │       ├── SearchResultDTO.java
│   │   │       ├── HybridSearchResultDTO.java
│   │   │       ├── RerankSearchResultDTO.java
│   │   │       ├── PipelineSearchResultDTO.java
│   │   │       └── EvaluationMetricsDTO.java
│   │   ├── entity/                  # 实体类
│   │   │   ├── Conversation.java    # 对话实体
│   │   │   ├── Message.java          # 消息实体
│   │   │   ├── FundLoan.java        # 借款实体
│   │   │   ├── FundRepayRecord.java # 还款记录实体
│   │   │   ├── MessageRole.java     # 消息角色枚举
│   │   │   ├── QueryEvaluation.java  # 查询评估实体
│   │   │   └── FundRepayRecord.java
│   │   └── repository/              # 数据访问层
│   │       ├── ConversationRepository.java
│   │       ├── FundLoanRepository.java
│   │       └── FundRepayRecordRepository.java
│   │
│   ├── exception/                 # 异常处理
│   │   ├── AiServiceException.java
│   │   └── GlobalExceptionHandler.java
│   │
│   └── AiDevelopApplication.java   # 应用启动类
│
├── src/main/resources/
│   ├── knowledge/                # 知识库文档 ⭐
│   │   ├── *.txt                 # 业务规则、产品手册等
│   │   └── *.pdf                 # PDF 文档
│   │
│   ├── application.yml           # 应用配置
│   └── static/                   # 静态资源
│       ├── css/
│       ├── js/
│       └── index.html
│
├── data/                        # 数据目录
│   └── vector-store.json         # 向量库存储文件
│
├── logs/                        # 日志目录
│   └── aidevelop.log
│
├── pom.xml                      # Maven 配置
└── README.md                    # 项目说明
```

### B. 技术架构演进路径

```
阶段1：基础 RAG
├── 向量检索
├── 知识库问答
└── 简单 LLM 回答

阶段2：高级 RAG（本系统）
├── 查询扩展
├── 查询重写
├── 混合检索
├── 重排序
└── 智能 RAG 管道

阶段3：企业级 RAG（待实现）
├── 多租户支持
├── 权限控制
├── 审计日志
├── A/B 测试框架
└── 实时监控告警

阶段4：前沿技术（待探索）
├── Agent（智能体）
├── ReAct（推理+行动）
├── GraphRAG（知识图谱）
└── CoT（思维链）
```

### C. 参考资源

#### 官方文档
- [Spring AI 官方文档](https://docs.spring.io/spring-ai/)
- [Spring AI GitHub](https://github.com/spring-projects/spring-ai)
- [智谱 AI 开放平台](https://open.bigmodel.cn/)

#### 学习资源
- 《RAG 检索增强生成实战》
- 《大语言模型应用开发实战》
- [Spring AI 示例项目](https://github.com/spring-projects/spring-ai)

#### 社区
- [Spring AI Discord](https://discord.gg/spring-ai)
- [智谱 AI 开发者社区](https://open.bigmodel.cn/dev/api)

---

## 文档版本

- **版本**：v1.0.0
- **更新日期**：2026-01-27
- **维护者**：AI Chat Assistant Team

---

## 联系方式

如有问题或建议，请通过以下方式联系：

- 项目 Issues：[GitHub Issues](https://github.com/your-repo/issues)
- 技术讨论：项目 Wiki 或 Discussion
- 邮件：support@example.com

---

**文档结束**
