# 06 - RAG 基础：向量检索与知识库构建

## 1. 什么是 RAG

Retrieval-Augmented Generation，即"检索增强生成"。核心思路是先从知识库中检索相关文档，再将检索结果作为上下文喂给 LLM，让 LLM 基于真实数据生成回答。

**为什么需要 RAG：** LLM 的训练数据有截止日期，且不包含企业私有数据。直接问 LLM 企业内部问题，它要么不知道，要么编造答案。RAG 让 LLM 能基于你的真实文档回答问题。

**RAG vs Fine-tuning：**

| 对比项 | RAG | Fine-tuning |
|--------|-----|-------------|
| 知识更新 | 实时，改文件即可 | 需要重新训练 |
| 成本 | 低，只需维护向量库 | 高，需要 GPU 训练 |
| 可追溯性 | 可返回来源文档 | 黑盒 |
| 适用场景 | 事实性问答、知识库 | 风格调整、领域适配 |

## 2. RAG 核心流程

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ 用户提问  │ -> │ 向量检索  │ -> │ 组合提示词 │ -> │ LLM 生成  │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
                    ↑
              ┌──────────┐
              │ 知识库    │ <- 文档 -> 分块 -> 嵌入 -> 存储
              └──────────┘
```

1. **离线阶段（建库）：** 原始文档经过分块、嵌入，以向量形式存入 VectorStore。
2. **在线阶段（检索）：** 用户提问被转换为向量，与库中向量做相似度计算，返回最相关的文档片段。
3. **生成阶段：** 将检索到的文档片段注入 Prompt，LLM 基于这些上下文生成回答。

## 3. 向量嵌入 (Embedding)

将文本转换为高维数值向量（如 1024 维浮点数组），使语义相近的文本在向量空间中距离也相近。

本项目使用 **智谱 AI embedding-3 模型**，理由：

- 中文语义理解优于 OpenAI 的 text-embedding 模型
- 价格低，适合学习和开发阶段
- 无需额外配置，Spring AI 的 ZhipuAI starter 直接支持

在 Spring AI 中，通过 `EmbeddingModel` 接口统一调用，项目使用 `@Qualifier("zhiPuAiEmbeddingModel")` 指定。

## 4. 文档加载与处理 (VectorStoreConfig)

文档入库的完整流水线在 `VectorStoreConfig` 中实现：

### 4.1 流程概览

```
classpath:knowledge/*.txt + *.pdf
        ↓ 加载
   List<Document>
        ↓ 添加元数据 (filename, type, source, fileType)
   List<Document> (enriched)
        ↓ TokenTextSplitter 切分
   List<Document> (split)
        ↓ EmbeddingModel 向量化
   List<Document> (with embeddings)
        ↓ SimpleVectorStore.add()
   持久化到 JSON 文件
```

### 4.2 关键实现细节

**加载阶段：** 使用 `TextReader` 加载 TXT 文件，`PagePdfDocumentReader` 按页加载 PDF 文件。通过 `@Value("classpath:knowledge/*.txt")` 通配符自动扫描知识库目录。

**元数据标注：** 根据文件名自动分类，`determineDocumentType()` 方法按文件名关键字匹配类型：

- 文件名含 `rules` 或 `rule` -> 类型 "规则"
- 文件名含 `product` 或 `manual` -> 类型 "产品"
- 文件名含 `risk` 或 `control` -> 类型 "风控"
- 文件名含 `contract` 或 `template` -> 类型 "合同"

**文档切分：** 使用 Spring AI 的 `TokenTextSplitter`，按 token 数量切分，确保每个片段不超过模型上下文限制。

**持久化：** 使用 `SimpleVectorStore`，向量数据保存为 JSON 文件（默认路径 `./data/vector-store.json`），启动时检测文件是否存在，存在则直接加载，否则重新构建。

### 4.3 核心代码结构

```java
@Bean
public VectorStore vectorStore(
    @Qualifier("zhiPuAiEmbeddingModel") EmbeddingModel embeddingModel
) {
    SimpleVectorStore vectorStore = new SimpleVectorStore(embeddingModel);
    if (vectorStoreFile.exists()) {
        vectorStore.load(vectorStoreFile);       // 已有缓存，直接加载
    } else {
        List<Document> docs = loadAndSplitDocuments();  // 加载、分块
        vectorStore.add(docs);                          // 向量化并存入
        vectorStore.save(vectorStoreFile);              // 持久化
    }
    return vectorStore;
}
```

## 5. 知识库文件结构

```
src/main/resources/knowledge/
├── product_manual.txt        # 产品手册 (type=产品)
├── loan_business_rules.txt   # 业务规则 (type=规则)
├── risk_control_guide.txt    # 风控指南 (type=风控)
└── 借款合同.pdf              # 合同模板 (type=合同)
```

文件名决定了元数据中的 `type` 字段，检索时可通过该字段做过滤。

## 6. 基础向量检索

### 6.1 两种使用方式

**方式一：QuestionAnswerAdvisor（自动注入）**

Spring AI 提供的 Advisor，自动将检索结果注入到 Prompt 中，适合直接在聊天流程中使用。

**方式二：手动检索**

直接调用 `VectorStore.similaritySearch()`，获取原始文档列表，适合需要对检索结果做二次处理的场景。

### 6.2 配置参数

在 `application.yml` 的 `app.chat.rag` 段配置，对应 `RagProperties` 类：

```yaml
app:
  chat:
    rag:
      similarity-threshold: 0.2   # 相似度阈值，ZhipuAI 建议设 0.2
      top-k: 5                    # 返回 Top-K 文档
```

`similarityThreshold` 的值设为 0.2 而非更高的原因：ZhipuAI 对"长文本片段 vs 短查询"的相似度分数通常偏低（0.2-0.3 范围），阈值过高会导致召回不足。

### 6.3 SearchRequest 构建模式

```java
SearchRequest request = SearchRequest.query(query)
    .withTopK(topK)
    .withSimilarityThreshold(threshold);
List<Document> results = vectorStore.similaritySearch(request);
```

## 7. API 接口

基础检索接口：

```
GET /api/chat/search?query=xxx&type=规则&topK=5
```

- `query`: 查询文本
- `type`: 可选，按文档类型过滤（规则/产品/风控/合同）
- `topK`: 可选，返回数量，默认 5

该接口内部会先调用 `QueryExpansionService` 扩展查询，再执行向量检索，提升召回率。

## 8. 动手实验

1. **添加新知识文档：** 在 `src/main/resources/knowledge/` 目录下放入新的 TXT 或 PDF 文件，删除 `./data/vector-store.json`，重启应用观察新文档是否被正确加载和检索。
2. **调整相似度阈值：** 将 `app.chat.rag.similarity-threshold` 从 0.2 改到 0.5 或 0.1，对比同一查询的检索结果数量和相关性变化。
3. **调整分块大小：** `TokenTextSplitter` 默认参数切分，可以尝试自定义实例化时传入不同的 `chunkSize`，观察小块 vs 大块对检索精度的影响。

## 9. 关键代码文件

| 文件 | 说明 |
|------|------|
| `src/main/java/.../config/VectorStoreConfig.java` | 向量库初始化、文档加载与切分 |
| `src/main/java/.../config/RagProperties.java` | RAG 配置属性（阈值、TopK） |
| `src/main/java/.../service/rag/QueryExpansionService.java` | 查询扩展（同义词） |
| `src/main/resources/knowledge/` | 知识库文件目录 |
| `src/main/resources/application.yml` | `app.chat.rag` 配置段 |
