# AI Agent 开发学习文档

基于 Spring Boot 3.3 + Spring AI 1.0.0-M5 的 AI Agent 开发实战教程。以金融贷款助手为业务场景，按难度递进覆盖从基础对话到 Agent Loop 的完整技术栈。

## 架构总览

```
┌─────────────────────────────────────────────────────┐
│                   表现层 (Controller)                 │
│         Chat API  /  RAG API  /  Cost API           │
├─────────────────────────────────────────────────────┤
│                   业务层 (Service)                    │
│   ChatService │ RagPipeline │ FunctionCalling       │
├─────────────────────────────────────────────────────┤
│                Spring AI 抽象层                       │
│   ChatModel │ VectorStore │ Advisor                │
├─────────────────────────────────────────────────────┤
│                   模型提供方                          │
│      DeepSeek(OpenAI) │ OpenAI-Compatible │ Ollama │
└─────────────────────────────────────────────────────┘
```

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
 ┌───────────────┐   ┌───────────────────┐
 │ 07 RAG 进阶   │ → │ 08 成本与可观测性  │
 │ 混合 + 管道   │   │ AOP + 缓存        │
 └───────────────┘   └───────────────────┘

第 5 周：深入理解
 ┌───────────────────┐
 │ 09 Embedding 与   │
 │ 文本分块策略       │
 └───────────────────┘

第 6 周：会话工程
 ┌───────────────────┐
 │ 10 Chat Memory    │
 │ 持久化 + 流式续聊   │
 └───────────────────┘
```

## 文档索引

| # | 文档 | 主题 | 难度 |
|---|------|------|------|
| 01 | [quick-start](01-quick-start.md) | 项目架构、技术栈、环境搭建 | ★ |
| 02 | [chat-basics](02-chat-basics.md) | ChatModel/ChatClient、SSE 流式、对话历史 | ★ |
| 03 | [multi-llm](03-multi-llm.md) | 多模型接入、Provider 抽象、Profile 切换 | ★★ |
| 04 | [prompt-engineering](04-prompt-engineering.md) | Prompt 模板、系统提示词、版本发布 | ★★ |
| 05 | [function-calling](05-function-calling.md) | AI 函数调用、工具注册与执行 | ★★★ |
| 06 | [rag-basics](06-rag-basics.md) | RAG 基础、向量嵌入、知识库构建 | ★★★ |
| 07 | [rag-advanced](07-rag-advanced.md) | 查询重写、混合检索(BM25+向量)、重排序 | ★★★ |
| 08 | [cost-and-observability](08-cost-and-observability.md) | AOP 调用日志、成本计算、Caffeine 缓存 | ★★ |
| 09 | [embedding-and-chunking](09-embedding-and-chunking.md) | 文本分块策略、Ollama 本地嵌入 | ★★★ |
| 10 | [chat-memory](10-chat-memory.md) | 会话持久化、SSE meta 事件、流式续聊 | ★★★ |

## 设计文档（持续落地）

| 文档 | 内容 |
|------|------|
| [agent-loop](design/agent-loop.md) | Agent Loop 架构（L3 已落地：Plan/Tool/Reflect/Replan/SelfCheck/Respond，风险问题要求 RAG 证据） |
| [enterprise-ai-evolution-todo](design/enterprise-ai-evolution-todo.md) | 企业级 AI 系统演进 TODO（Embedding 升级暂缓） |

## 当前实现边界

- `/api/chat` — 主对话接口，可启用内置 Advisor（基础 RAG）
- `/api/rag` — 高级 RAG 实验（混合检索、重排、评估）
- `/api/agent/chat` — Agent Loop MVP（Plan -> Tool -> Respond，含 traceId 与步骤追踪）
- 对话历史 — 基于 MySQL `chat_message` 表持久化，重启后可恢复
