# Embedding 与文本切分策略

## 概述

在 RAG（检索增强生成）系统中，文本切分是**最影响检索质量的环节**。切分决定了"信息的颗粒度"——embedding 模型再强，如果喂给它的片段语义不完整或噪音太多，检索结果也不会好。

## 本项目的 Embedding 流程

```
┌─────────────────── 启动时（一次性） ───────────────────┐
│                                                        │
│  knowledge/*.txt,pdf                                   │
│       │                                                │
│       ▼                                                │
│  TextReader / PdfReader → List<Document>               │
│       │                                                │
│       ▼                                                │
│  TokenTextSplitter (500 token/chunk) → 切分后的片段     │
│       │                                                │
│       ▼                                                │
│  Ollama nomic-embed-text → 每个片段得到 768 维向量      │
│       │                                                │
│       ▼                                                │
│  SimpleVectorStore → 持久化为 vector-store-ollama.json  │
│                                                        │
└────────────────────────────────────────────────────────┘

┌─────────────────── 每次用户提问 ─────────────────────┐
│                                                        │
│  用户问题 → Ollama embed → 问题向量                     │
│       │                                                │
│       ▼                                                │
│  VectorStore.similaritySearch(问题向量, topK=5)         │
│       │ 余弦相似度排序                                  │
│       ▼                                                │
│  返回最相关的 5 个文档片段                               │
│       │                                                │
│       ▼                                                │
│  QuestionAnswerAdvisor 拼接到 prompt → 发给 LLM        │
│                                                        │
└────────────────────────────────────────────────────────┘
```

## 切分策略详解

### 为什么切分很重要

```
切分太大（2000 token）：
  → 片段包含多个主题，向量是"平均语义"，检索时什么都像又什么都不像
  → 注入到 prompt 里占太多 token，浪费 LLM 上下文窗口

切分太小（100 token）：
  → 片段缺乏上下文，一句话脱离前后文后语义不完整
  → 检索到了但 LLM 看不懂这个片段在说什么

切在错误的位置：
  → "逾期还款将按日收取万分之" | "五的罚息" ← 关键信息被撕裂
```

### 主流切分策略对比

| 策略 | 原理 | 适用场景 |
|------|------|----------|
| **固定 token 数** | 按 token 计数硬切 | 通用，但粗暴 |
| **递归字符切分** | 按分隔符优先级递归（段落→句子→字符） | 通用文本，LangChain 默认 |
| **语义切分** | 用 embedding 检测语义断点 | 长文档、主题混杂 |
| **文档结构切分** | 按标题/章节/段落自然边界 | 结构化文档（Markdown、法规） |
| **滑动窗口 + 重叠** | 固定窗口但相邻片段有重叠 | 防止边界信息丢失 |

### 本项目的选择：TokenTextSplitter

使用 Spring AI 内置的 `TokenTextSplitter`，基于 token 计数切分，并在标点处优化截断位置。

**配置参数（见 `VectorIndexBuilder.java`）：**

```java
TokenTextSplitter.builder()
    .withChunkSize(500)           // 目标 500 token ≈ 200-350 中文字
    .withMinChunkSizeChars(200)   // 至少 200 字符才在标点处截断
    .withMinChunkLengthToEmbed(10) // 短于 10 字符的片段丢弃
    .withMaxNumChunks(10000)      // 单文档最多 10000 个片段
    .withKeepSeparator(true)      // 保留换行符，维持段落结构
    .build();
```

**参数选择理由：**

- **chunkSize=500**：中文在 CL100K_BASE tokenizer 下，1 个汉字约 1-3 个 token。500 token 约 200-350 个汉字，接近一个自然段落的长度，语义完整性好。
- **minChunkSizeChars=200**：确保在句号处截断时，片段不会太短。避免出现只有一两句话的碎片。
- **keepSeparator=true**：金融文档通常有条目结构（换行分隔），保留换行有助于 LLM 理解片段的结构。

### 切分算法流程

```
原始文本（假设 2000 tokens）
    │
    ▼
1. 用 CL100K_BASE 编码器把文本转为 token 序列
    │
    ▼
2. 取前 500 个 token，解码回文本
    │
    ▼
3. 如果还有剩余 token：
   → 在这段文本中找最后一个 。？！\n
   → 如果找到且位置 > 200 字符 → 在那里截断（保证语义完整）
   → 如果没找到 → 按 500 token 硬切
    │
    ▼
4. 移除已切出的 token，回到步骤 2
    │
    ▼
5. 重复直到 token 序列为空或达到 maxNumChunks
```

## Embedding 模型

本项目使用 **Ollama nomic-embed-text**（768 维向量）。

关键约束：**文档入库和用户查询必须使用同一个 embedding 模型**。不同模型产生的向量在不同的语义空间中，计算相似度无意义。切换模型后必须重建向量库（删除 JSON 文件重启即可）。

## 检索参数

配置在 `application.yml` 的 `app.chat.rag` 下：

| 参数 | 值 | 含义 |
|------|-----|------|
| `top-k` | 5 | 最多返回 5 个最相似片段 |
| `similarity-threshold` | 0.2 | 余弦相似度低于 0.2 的丢弃 |

## 优化方向

### 已实现
- metadata 标记文档类型（规则/产品/风控/合同）
- 保留段落结构（keepSeparator）
- 适当的 chunk size（500 token）

### 可进一步优化
1. **Overlap（重叠切分）**：相邻片段共享部分文本，防止边界信息丢失。TokenTextSplitter 不支持，需自定义实现。
2. **Metadata Filter**：检索时按文档类型过滤，如用户问风控问题只在风控文档中搜索。
3. **按文档结构切分**：如果知识库有明确条目（"第一条：..."），按条目边界切分比按 token 数更精准。
4. **语义切分**：用 embedding 相似度检测段落间的语义跳变点，在语义断裂处切分。适合长文档。

## 相关代码

- 索引构建：`src/main/java/com/example/aidevelop/config/VectorIndexBuilder.java`
- 向量库配置：`src/main/java/com/example/aidevelop/config/VectorStoreConfig.java`
- RAG 装配：`src/main/java/com/example/aidevelop/config/AiModelConfig.java`
- 检索参数：`src/main/resources/application.yml` → `app.chat.rag`
