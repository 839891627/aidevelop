# 架构演进路线

本文档记录当前项目已经完成的工程化能力，以及后续如果继续往生产级 AI 应用演进，最值得补齐的方向。

## 1. 当前已完成能力

| 方向 | 当前状态 |
|---|---|
| Chat 模式分流 | 已支持 `general`、`financial_rag`、`auto` |
| RAG 入口统一 | 已通过 `RagFacade` 统一 Chat 和 Agent RAG 调用 |
| Agent Runtime | 已支持 Plan、Tool、Reflect、Replan、Respond、SelfCheck |
| 结构化输出 | 已通过 `AgentStructuredOutputValidator` 统一校验 |
| 超时控制 | Agent LLM 调用已通过 `AgentLlmClient` 设置超时 |
| 预算控制 | 已有 `AgentBudgetTracker` 记录每轮调用预算 |
| 资源限流 | 已有 `AgentRateLimiter` 单机令牌桶，LLM 全局 + 工具按名分桶 |
| 步骤状态 | 已有 `AgentStepStatus` |
| 失败分类 | 已有 `AgentFailureReason` |
| Trace 持久化 | 已有 `agent_trace`、`agent_step` 和查询接口 |
| 成本观测 | 已有 `ai_call_log`、token/cost 统计和 `traceId` 关联 |
| Prompt 治理 | 已有 Prompt Registry、草稿、发布、回滚 |
| 多 Agent | 已有 Supervisor、SubAgent、共享状态和工具白名单 |

## 2. 仍然值得补齐的短板

### P0：RAG 质量评测

当前 RAG 工程链路已经统一，但缺少离线评测闭环。建议优先补：

- 固定 50~100 条金融业务问答评测集
- Recall@K、MRR、NDCG、空召回率
- 每次修改分块、embedding、检索策略后跑回归
- 记录查询重写、检索、重排、生成各阶段耗时

原因：没有评测基线，就很难证明 RAG 优化真的有效。

### P0：安全与审计

当前项目更偏工程样板，还没有完整生产安全边界。建议补：

- API 鉴权
- 管理接口 RBAC
- Prompt 发布审计增强
- 聊天内容、日志、Prompt 中的敏感信息脱敏
- 关键操作审计日志

原因：金融场景天然涉及敏感数据，安全治理是企业 AI 项目的必要条件。

### P1：预算从调用次数扩展到 token / 金额

`AgentBudgetTracker` 已经具备预算入口，但当前更偏调用次数控制。后续可以扩展为：

- 每轮 token 上限
- 每次请求 token 上限
- 每次请求金额上限
- 超预算自动降级或停止
- 按 `traceId` 汇总一次 Agent 请求总成本

原因：Agent 和多 Agent 链路天然会放大调用次数，成本预算必须前置。

> 补充：跨请求的速率限流（`AgentRateLimiter`）已落地单机令牌桶版本，保护 LLM provider 和工具后端；多实例部署时仍需升级为 Redis 分布式限流，并可叠加基于 P99 延迟/错误率的自适应限流。

### P1：数据库迁移工程化

当前 SQL 脚本适合本地演示。继续演进时建议引入：

- Flyway 或 Liquibase
- schema 版本记录
- 启动时自动迁移
- 不同环境的迁移策略

原因：Prompt、成本、trace、业务表越来越多后，手工执行 SQL 风险会变高。

### P1：Agent 质量评估

当前已经能回放 Agent steps，但还缺少质量评估闭环。建议补：

- 工具调用准确率
- Replan 次数和失败率
- SelfCheck 通过率
- 常见失败原因分布
- 多 Agent 调度轮数和成本分布

原因：有 trace 只是可观测，能基于 trace 做质量分析才是治理闭环。

## 3. 暂不优先做的方向

| 方向 | 原因 |
|---|---|
| 更换 Embedding 模型 | 先建立评测基线，否则无法判断升级收益 |
| 引入复杂工作流引擎 | 当前 Agent Runtime 已足够展示核心能力 |
| 大规模重构前端 | 项目重点是 Java 后端和 AI 应用架构 |
| 微服务拆分 | 当前单体更适合求职展示，模块边界已经足够清晰 |

## 4. 推荐阶段计划

### Phase 1：补质量基线

目标：让 RAG 和 Agent 的效果可以被度量。

交付物：

- RAG 离线评测集
- Agent 典型任务集
- 质量指标脚本
- 成本与质量对照报告

### Phase 2：补安全治理

目标：让项目更接近企业生产场景。

交付物：

- API 鉴权
- 管理接口权限
- Prompt 发布审计
- 敏感信息脱敏

### Phase 3：补预算闭环

目标：把 Agent 的成本控制从“可观察”升级为“可约束”。

交付物：

- token / cost 级预算
- 超预算中止和降级策略
- `traceId` 级成本聚合
- 成本告警规则

## 5. 面试表达

如果被问“后续怎么优化”，建议这样回答：

> 这个项目现在已经完成了 AI 应用的主链路和 Agent 工程化闭环。下一步我不会先追求更复杂的模型或框架，而会优先补质量评测、安全审计和成本预算，因为这些才是企业 AI 应用从 Demo 走向生产时最关键的能力。
