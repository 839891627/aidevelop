# 项目文档

文档按“架构说明、使用指南、执行示例”三类组织，便于面试讲解和后续维护。

## 推荐阅读顺序

1. [面试讲解指南](architecture/interview-guide.md)：先掌握项目介绍、亮点和架构取舍。
2. [架构总览](architecture/overview.md)：理解系统边界和整体模块。
3. [Agent Loop](architecture/agent-loop.md)：重点看 Agent Runtime 的工程化设计。
4. [RAG 指南](guides/rag.md)：理解统一 `RagFacade` 如何服务 Chat 和 Agent。
5. [多 Agent](architecture/multi-agent.md)：理解 Supervisor / SubAgent 的分工。
6. [成本观测](guides/cost-observability.md)：理解 traceId 如何关联模型调用成本。
7. [执行链路示例](examples/execution-flows.md)：用于面试时按请求链路讲解。

## 目录结构

```text
docs/
├── README.md
├── architecture/
│   ├── overview.md
│   ├── interview-guide.md
│   ├── agent-loop.md
│   ├── multi-agent.md
│   └── evolution-roadmap.md
├── guides/
│   ├── quick-start.md
│   ├── chat.md
│   ├── rag.md
│   ├── multi-llm.md
│   ├── function-calling.md
│   ├── prompt-engineering.md
│   └── cost-observability.md
└── examples/
    └── execution-flows.md
```

## 文档清单

| 分类 | 文档 | 内容 |
|---|---|---|
| 架构 | [面试讲解指南](architecture/interview-guide.md) | 项目介绍、亮点、取舍和追问准备 |
| 架构 | [架构总览](architecture/overview.md) | 当前系统架构、模块边界、核心链路 |
| 架构 | [Agent Loop](architecture/agent-loop.md) | Agent Runtime、结构化输出、预算、超时、限流、trace |
| 架构 | [多 Agent](architecture/multi-agent.md) | Supervisor、SubAgent、工具白名单、Agent-as-Tool |
| 架构 | [演进路线](architecture/evolution-roadmap.md) | 后续可优化方向 |
| 指南 | [快速开始](guides/quick-start.md) | 环境变量、数据库、启动和演示路径 |
| 指南 | [Chat](guides/chat.md) | ChatMode、SSE、会话记忆、RAG 注入 |
| 指南 | [RAG](guides/rag.md) | `RagFacade`、Pipeline、知识库构建 |
| 指南 | [多模型接入](guides/multi-llm.md) | Chat / Embedding / Agent LLM 配置 |
| 指南 | [Function Calling](guides/function-calling.md) | 工具函数和业务工具 |
| 指南 | [Prompt 工程](guides/prompt-engineering.md) | Prompt Registry 和版本治理 |
| 指南 | [成本观测](guides/cost-observability.md) | `ai_call_log`、Agent Trace、成本统计 |
| 示例 | [执行链路示例](examples/execution-flows.md) | 常规 Chat、金融 RAG、单 Agent、多 Agent 链路 |
