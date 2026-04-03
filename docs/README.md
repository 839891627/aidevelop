# AI Agent 开发学习文档

本项目是 Java 开发者学习 AI Agent 开发的实战教程，基于 Spring Boot 3.3 + Spring AI 构建。每个文档对应一个独立的知识点，按难度递进排列。

## 学习路线

```
第 1 周：基础入门
 ┌───────────────┐   ┌───────────────┐
 │ 01 快速开始    │ → │ 02 基础对话    │
 │ 架构 + 环境    │   │ 流式 + 历史    │
 └───────────────┘   └───────────────┘

第 2 周：核心技能
 ┌───────────────┐   ┌───────────────┐
 │ 03 多模型接入  │ → │ 04 Prompt 工程 │
 │ Profile 切换   │   │ 模板 + 提示词  │
 └───────────────┘   └───────────────┘

第 3 周：Agent 能力
 ┌───────────────┐   ┌───────────────┐
 │ 05 Function   │ → │ 06 RAG 基础    │
 │ Calling       │   │ 向量 + 知识库  │
 └───────────────┘   └───────────────┘

第 4 周：进阶优化
 ┌───────────────┐   ┌───────────────┐
 │ 07 RAG 进阶   │ → │ 08 成本与运维  │
 │ 混合 + 管道   │   │ AOP + 缓存    │
 └───────────────┘   └───────────────┘
```

## 文档索引

| 文档 | 主题 | 难度 | 关键代码文件 |
|------|------|------|------------|
| [01-quick-start.md](01-quick-start.md) | 项目架构总览与快速开始 | ★ | `AiDevelopApplication.java`, `pom.xml`, `application.yml` |
| [02-chat-basics.md](02-chat-basics.md) | 基础对话、流式响应、对话历史 | ★ | `ChatServiceImpl.java`, `ChatController.java`, `Conversation.java` |
| [03-multi-llm.md](03-multi-llm.md) | 多 LLM 接入、Profile 切换 | ★★ | `AiModelConfig.java`, `application-*.yml` |
| [04-prompt-engineering.md](04-prompt-engineering.md) | Prompt 模板管理、系统提示词 | ★★ | `PromptService.java`, `PromptProperties.java` |
| [05-function-calling.md](05-function-calling.md) | AI 函数调用、工具使用 | ★★★ | `LoanQueryFunction.java`, `RiskAssessmentFunction.java` |
| [06-rag-basics.md](06-rag-basics.md) | RAG 基础、向量检索、知识库 | ★★★ | `VectorStoreConfig.java`, `RagProperties.java` |
| [07-rag-advanced.md](07-rag-advanced.md) | 查询重写、混合检索、重排序、管道 | ★★★ | `BM25Service.java`, `HybridSearchService.java`, `RagPipelineService.java` |
| [08-cost-and-observability.md](08-cost-and-observability.md) | 成本管理、AOP 日志、缓存 | ★★ | `AiCallLoggerAspect.java`, `CacheConfig.java`, `AiCostStatisticsService.java` |
| [AI_LEARNING_PATH.md](AI_LEARNING_PATH.md) | 4 周学习路线图 | - | 全部 |

## 旧文档存档

以下文档已整合到上述专题中，保留供参考：
- `LEARNING_JOURNEY_old.md` - 原始学习笔记（1839 行，部分过时）
- `RAG_ARCHITECTURE_old.md` - RAG 架构详细文档（1261 行，内容优秀）
