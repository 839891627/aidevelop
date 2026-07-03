# 面试讲解指南

本文档用于把项目讲成一条清晰的面试主线。目标不是逐个文件介绍代码，而是说明：这个项目解决什么问题、架构为什么这样拆、哪些地方体现了企业 AI 应用工程化能力。

## 1. 一句话介绍

可以这样开场：

> 这是一个金融信贷场景下的企业 AI 应用工程化样板。它不是单纯调用大模型接口，而是围绕 Chat、RAG、Agent、多 Agent、Prompt 治理、成本观测和 Trace 回放，搭了一套可控制、可观测、可扩展的后端架构。

## 2. 推荐讲解顺序

1. **先讲业务场景**：金融信贷问答、借款/还款查询、风险评估。
2. **再讲能力分层**：普通 Chat、金融 RAG、Agent 任务不是混在一条链路里。
3. **重点讲统一 RAG**：Chat 和 Agent 共用 `RagFacade`，避免两套检索逻辑。
4. **重点讲 Agent Runtime**：结构化输出校验、预算、超时、步骤状态、失败原因、trace 持久化。
5. **补充多 Agent**：复杂跨域任务由 Supervisor 调度子 Agent，子 Agent 通过工具白名单隔离能力。
6. **最后讲治理能力**：Prompt Registry、成本统计、`traceId` 关联模型调用。

## 3. 项目亮点

| 亮点 | 面试表达 |
|---|---|
| ChatMode 显式分流 | 普通聊天、金融 RAG、Agent 任务不混用同一套 prompt 和工具 |
| 统一 RAG Facade | 移除了公开 RAG 调试链路，生产能力统一收口到 `RagFacade` |
| Agent Runtime 强化 | LLM 输出必须结构化校验，调用有超时、预算和限流，步骤有状态和失败原因 |
| Trace 持久化 | Agent 响应返回 `traceId`，可回放 PLAN/TOOL/REFLECT/RESPOND 等步骤 |
| 成本可观测 | `ai_call_log` 记录 token、耗时、费用，并通过 `traceId` 关联 Agent 阶段 |
| 多 Agent 编排 | Supervisor 负责调度，SubAgent 复用单 Agent Runtime，工具权限按角色隔离 |
| Prompt 治理 | Prompt 从代码中剥离，支持草稿、发布、回滚和按环境读取 |

## 4. 架构取舍

### 为什么不把所有能力挂到一个 ChatClient 上？

因为普通聊天、金融知识库问答和业务工具任务的约束不同。如果全局挂载金融 prompt、RAG 和工具，普通问题也可能被误导到金融语境，模型还可能错误调用业务工具。

当前做法是：`ChatMode` 和 `IntentRoutingService` 先判断模式，再由 `ChatServiceImpl` 按需拼接 prompt、RAG context 或工具。

### 为什么去掉公开 RAG 调试接口？

公开 RAG 调试接口适合早期调试，但长期存在会让 Chat RAG、Agent RAG 和调试 RAG 形成多条行为不一致的路径。现在生产链路统一走 `RagFacade`，RAG 作为内部能力被 Chat 和 Agent 复用。

### 为什么 Agent 不直接相信 LLM 输出？

Agent 里 LLM 的输出会决定工具调用和下一步流程。如果直接解析模型文本，一旦模型多输出解释、字段缺失或 JSON 格式漂移，就可能导致链路不稳定。

因此当前通过 `AgentStructuredOutputValidator` 统一提取和校验 JSON，并把解析失败分类为明确的失败原因。

### 为什么需要多 Agent？

单 Agent 适合处理简单工具任务；多 Agent 适合跨域问题，例如同时需要借款数据、还款数据、知识库规则和风险判断。多 Agent 的价值不在于“更多模型调用”，而在于职责分离、工具隔离和可观测的分工链路。

### 为什么 Agent 要单独做限流？

普通接口限流防的是"压垮系统"，Agent 限流要防三件事：一次用户请求会被放大成多次 LLM/工具调用（调用放大）；单请求耗时几十秒到两分钟，长占用线程和连接（慢响应）；LLM 按 token 计费，限流同时是防账单失控（成本敏感）；LLM provider 和工具后端还有 RPM/TPM 硬配额（下游脆）。

因此限流分两层：`AgentBudgetTracker` 限单请求内部不失控，`AgentRateLimiter` 限跨请求全局不超载。实现上 LLM 全局一桶 + 工具按名分桶，每个桶同时控 QPS（令牌桶）和并发（`Semaphore`），拿不到令牌先排队、超时再走兜底回答。默认单机令牌桶，接口预留了 Redis 分布式实现。

## 5. 典型演示问题

| 场景 | 示例问题 | 展示点 |
|---|---|---|
| 常规 Chat | `解释一下什么是 RAG` | `mode=general` 不启用金融 RAG |
| 金融 RAG | `提前还款有什么规则？` | `RagFacade` 检索证据并注入 prompt |
| 单 Agent | `查询 CUST1001 的借款和还款情况` | Plan -> Tool -> Reflect -> Respond |
| 风险评估 | `结合 CUST1001 的还款情况和规则评估风险` | 业务工具 + `rag.search` 证据 |
| 多 Agent | `查询借款还款，再结合风控规则给出综合风险判断` | Supervisor 调度多个子 Agent |
| Trace 回放 | 使用上一步返回的 `traceId` 查询 trace | 执行步骤、失败原因、模型调用关联 |

## 6. 面试追问准备

### 如果面试官问“项目哪里最有工程含量？”

优先讲 Agent Runtime：

- LLM 调用统一走 `AgentLlmClient`
- 输出统一走 `AgentStructuredOutputValidator`
- 每轮执行有 `AgentBudgetTracker`，跨请求有 `AgentRateLimiter` 限流
- 步骤有 `AgentStepStatus` 和 `AgentFailureReason`
- 结果由 `AgentTraceService` 持久化

这能体现你不是只会调 API，而是在做可控、可观测的 AI 应用后端。

### 如果面试官问“RAG 做得怎么样？”

重点讲统一入口和检索链路：

- `RagFacade` 收口生产调用
- `RagRequest` / `RagRetrievalResult` 统一输入输出
- Pipeline 支持查询重写、查询扩展、向量检索、BM25 混合检索和重排扩展
- 无证据时明确降级，避免模型假装引用知识库

### 如果面试官问“还有什么不足？”

可以坦诚说当前仍有演进空间：

- RAG 缺少离线评测集和召回指标看板
- `AgentBudgetTracker` 目前偏调用次数控制，后续应扩展到 token 和金额预算
- 资源限流目前是单机令牌桶，多实例部署应升级为 Redis 分布式限流，并可叠加基于延迟/错误率的自适应限流
- 安全治理还可以补 API 鉴权、脱敏、租户隔离
- 数据库迁移可以从 SQL 脚本升级为 Flyway/Liquibase

这样的回答比“已经完全生产级”更可信。
