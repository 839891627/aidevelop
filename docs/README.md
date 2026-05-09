# AI Agent 开发学习文档

本项目是 Java 开发者学习 AI Agent 开发的实战教程，基于 Spring Boot 3.3 + Spring AI 1.0.0-M5 构建。每个文档对应一个独立的知识点，按难度递进排列。

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
| [00-ai-agent-system-blueprint.md](00-ai-agent-system-blueprint.md) | 系统化蓝图：架构、能力地图与演进路线 | ★ | 全局总览（无代码实现） |
| [AI_AGENT_COMPLETE_GUIDE.md](AI_AGENT_COMPLETE_GUIDE.md) | 单篇完整压缩版（覆盖全主题，便于连续阅读） | ★ | 体系化整合（弱代码细节） |
| [01-quick-start.md](01-quick-start.md) | 项目架构总览与快速开始 | ★ | `AiDevelopApplication.java`, `pom.xml`, `application.yml` |
| [02-chat-basics.md](02-chat-basics.md) | 基础对话、流式响应、对话历史 | ★ | `ChatServiceImpl.java`, `ChatController.java`, `Conversation.java` |
| [03-multi-llm.md](03-multi-llm.md) | 多 LLM 接入、Profile 切换 | ★★ | `AiModelConfig.java`, `application-*.yml` |
| [04-prompt-engineering.md](04-prompt-engineering.md) | Prompt 模板管理、系统提示词 | ★★ | `PromptService.java`, `PromptProperties.java` |
| [05-function-calling.md](05-function-calling.md) | AI 函数调用、工具使用 | ★★★ | `LoanQueryFunction.java`, `RiskAssessmentFunction.java` |
| [06-rag-basics.md](06-rag-basics.md) | RAG 基础、向量检索、知识库 | ★★★ | `VectorStoreConfig.java`, `RagProperties.java` |
| [07-rag-advanced.md](07-rag-advanced.md) | 查询重写、混合检索、重排序、管道 | ★★★ | `BM25Service.java`, `HybridSearchService.java`, `RagPipelineService.java` |
| [08-cost-and-observability.md](08-cost-and-observability.md) | 成本管理、AOP 日志、缓存 | ★★ | `AiCallLoggerAspect.java`, `CacheConfig.java`, `AiCostStatisticsService.java` |
| [10-agent-loop-design.md](10-agent-loop-design.md) | 从 Chat+RAG 升级到 Agent Loop 的架构设计 | ★★★★ | `agent/*`（规划新增） |
| [ALL_IN_ONE_KNOWLEDGE_BASE_OBSIDIAN_LITE.md](ALL_IN_ONE_KNOWLEDGE_BASE_OBSIDIAN_LITE.md) | Obsidian 精简版总览（结构化学习地图） | ★ | 全局速览（无实现细节） |
| [AI_LEARNING_PATH.md](AI_LEARNING_PATH.md) | 4 周学习路线图 | - | 全部 |

## 当前实现边界（学习前必读）

- 对话接口：`/api/chat`，可按配置启用内置 Advisor（基础 RAG）。
- 高级 RAG 接口：`/api/rag`，用于混合检索、重排、评估等实验能力。
- 对话历史存储：`ConversationRepository` 当前为内存实现，重启后会清空。
