# 从 Chat+RAG 到 Agent Loop 设计文档

## 1. 背景与目标
当前项目已经具备：
- 多模型对话（普通 + 流式）
- Function Calling（贷款/还款/风控）
- RAG（查询重写、扩展、混合检索、重排、评估）

下一阶段目标是将能力升级为可迭代的 Agent Loop：
- 让系统具备“规划 -> 工具调用 -> 观察 -> 反思 -> 输出”的闭环
- 让每一轮执行可观测、可回放、可评估
- 保持与现有 `ChatService` / `RagPipelineService` / Function 模块兼容

## 2. Agent Loop 定义

推荐采用最小可用闭环（MVP）：

1) **Plan**：基于用户输入生成执行计划（可只生成 1~3 步）  
2) **Act**：调用工具（RAG 检索、函数调用、系统工具）  
3) **Observe**：收集工具返回结果并结构化  
4) **Reflect**：判断是否继续调用工具、是否已可回答  
5) **Respond**：输出最终答案并附带可选推理摘要  

终止条件：
- 达到最大步数（默认 6 步）
- LLM 给出 `done=true`
- 发生不可恢复错误

## 3. 目标架构

建议新增 `agent` 分层：

```text
com.example.aidevelop.agent
├── controller
│   └── AgentController.java
├── service
│   ├── AgentLoopService.java
│   ├── AgentPlanner.java
│   ├── AgentExecutor.java
│   ├── AgentReflector.java
│   └── ToolRouter.java
├── model
│   ├── AgentRequest.java
│   ├── AgentResponse.java
│   ├── AgentStep.java
│   ├── AgentState.java
│   └── ToolCall.java
└── tool
    ├── AgentTool.java
    ├── RagSearchTool.java
    ├── LoanQueryTool.java
    └── RepaymentQueryTool.java
```

## 4. 核心数据模型（建议）

- `AgentRequest`
  - `message`
  - `conversationId`
  - `maxSteps`（默认 6）
  - `enableTrace`（默认 true）

- `AgentState`
  - `traceId`
  - `stepIndex`
  - `plan`
  - `lastObservation`
  - `finalAnswer`
  - `finished`

- `AgentStep`
  - `stepIndex`
  - `actionType`（PLAN/TOOL/REFLECT/RESPOND）
  - `toolName`
  - `toolInput`
  - `toolOutput`
  - `latencyMs`
  - `success`
  - `errorMessage`

## 5. 工具抽象与路由

新增统一工具接口：

```java
public interface AgentTool {
    String name();
    ToolResult execute(Map<String, Object> args);
}
```

`ToolRouter` 维护 `name -> AgentTool` 映射：
- `rag.search` -> `RagPipelineService.search(...)`
- `loan.query` -> `LoanQueryFunction.apply(...)`
- `repayment.query` -> `RepaymentQueryFunction.apply(...)`

这样后续可扩展：
- `cost.today`
- `prompt.get`
- 外部 API 工具

## 6. 与现有系统的集成方式

- 保留原 `/api/chat` 和 `/api/chat/stream`，不破坏现有前端
- 新增 `/api/agent/chat`：
  - 同步模式：返回最终答案 + steps 摘要
  - 可选流式模式：按 step 输出事件
- RAG 与 Function 不重写，只通过 `ToolRouter` 复用

## 7. 可观测性与评估

每次 Agent 请求必须生成 `traceId`，并记录：
- 总步数
- 每步耗时
- 工具成功率
- Token 使用量
- 是否命中 RAG / 调用了哪些工具

建议新增评估集（JSONL）：
- `query`
- `expectedTools`
- `expectedFacts`
- `mustIncludeKeywords`

每次策略/Prompt 改动后批量评测，输出：
- 工具调用正确率
- 首答正确率
- 平均步数
- 平均耗时

## 8. 风险与约束

- 避免无限循环：必须有 `maxSteps`
- 避免过度调用工具：Reflect 阶段增加“是否已有足够证据”的判断
- 避免提示词注入：工具参数做白名单校验
- 避免成本失控：对每轮调用设置 token 上限和超时

## 9. 分阶段实施计划

### Phase 1（MVP，1-2 天）
- 新增 `agent` 包结构和核心模型
- 打通 `Plan -> Tool -> Respond`（先不做复杂 Reflect）
- 接入 `rag.search` 与 `loan.query`

### Phase 2（增强，2-3 天）
- 增加 Reflect 阶段
- 增加失败重试策略与步内超时
- 增加 step 级日志与 traceId

### Phase 3（评估与优化，2 天）
- 建立 `agent-eval` 数据集
- 加入回归评测命令
- 根据指标调参（maxSteps、tool 触发策略）

## 10. 验收标准

- 能处理“需先检索再回答”的复杂问题
- 能处理“需调用业务函数”的问题
- `traceId` 可串联整轮执行日志
- 至少 20 条评测样本可重复跑并输出指标

---

该设计文档优先保证“可落地”，避免一次性引入过重框架；建议先跑通 MVP，再逐步演进成多 Agent 架构。
