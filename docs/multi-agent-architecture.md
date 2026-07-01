# 多 Agent 架构文档

## 概述

本项目在原有单 Agent Loop 基础上，新增了多 Agent 协作能力。当用户提出跨领域复杂问题时，系统自动调度多个专业化子 Agent 协同完成任务。

核心设计原则：
- **向后兼容**：单意图请求走原有 AgentLoopService，行为不变
- **最小侵入**：现有核心类（Planner/Executor/Reflector/Responder）零修改
- **YAML 驱动**：子 Agent 定义、工具白名单、调度策略全部配置化

---

## 架构总览

```
POST /api/agent/chat
        │
        ▼
┌─────────────────────────────────────────────────┐
│  AgentDispatcher (@Primary AgentService)         │
│                                                  │
│  判断逻辑:                                        │
│  1. request.multiAgent == true  → 多 Agent       │
│  2. request.multiAgent == false → 单 Agent       │
│  3. null → 由 IntentRoutingService 自动决定       │
└───────────┬─────────────────────────┬────────────┘
            │                         │
   RouteType != MULTI_AGENT    RouteType == MULTI_AGENT
            │                         │
            ▼                         ▼
┌───────────────────┐    ┌──────────────────────────┐
│  AgentLoopService │    │  SupervisorOrchestrator   │
│  (原有，不动)      │    │                          │
│                   │    │  LLM 调度循环:            │
│  Plan→Tool→       │    │  decideNext() → DISPATCH │
│  Reflect→Respond  │    │  → SubAgentRunner        │
│                   │    │  → 循环直到 FINISH        │
└───────────────────┘    │  → synthesize() 综合回答  │
                         └──────────────────────────┘
                                    │
                         ┌──────────┼──────────┐
                         ▼          ▼          ▼
                    loan-agent  risk-agent  rag-agent
                    (各自独立的 AgentLoop 实例)
```

---

## 两种多 Agent 模式

### 模式一：Supervisor 编排（推荐）

适合复杂跨域任务。Supervisor 是一个 LLM 调度器，自主决定调用哪个子 Agent。

**流程：**
1. Supervisor LLM 根据用户问题 + 已有结果，输出 `{"action":"DISPATCH","targetAgent":"xxx"}`
2. 被选中的子 Agent 独立执行（有自己的工具白名单、步数限制、反思策略）
3. 子 Agent 结果写入共享状态 `MultiAgentState`
4. Supervisor 再次决策：继续 DISPATCH 还是 FINISH
5. FINISH 后由 LLM 综合所有子 Agent 结果生成最终回答

**触发条件：**
- 消息包含 `multi-agent-keywords` 中的关键词（如"综合评估"）
- 或消息同时命中 tool 类关键词 + rag 类关键词（跨域）
- 或请求体显式指定 `"multiAgent": true`

### 模式二：Agent-as-Tool（轻量）

适合单 Agent 模式下偶尔需要委派子任务。现有 Planner 可以像调用普通工具一样调用子 Agent。

**使用方式：**

Planner 输出：
```json
{"toolCalls": [{"toolName": "agent.delegate", "args": {"agentName": "risk-agent", "query": "评估 CUST001 风险"}}]}
```

SubAgentTool 自动注册到 ToolRouter，被当作普通工具执行。

---

## 文件结构

```
src/main/java/com/example/aidevelop/agent/multi/
├── MultiAgentProperties.java      ← YAML 配置绑定
├── SubAgentDefinition.java        ← 子 Agent 定义（record）
├── MultiAgentState.java           ← 跨 Agent 共享状态
├── SubAgentExecution.java         ← 子 Agent 执行记录（record）
├── SupervisorDecision.java        ← 调度决策（record）
├── SubAgentRunner.java            ← 子 Agent 执行器（复用现有 Loop 逻辑）
├── SupervisorOrchestrator.java    ← LLM 调度 + 循环控制
├── AgentDispatcher.java           ← @Primary 路由分流
└── SubAgentTool.java              ← Agent-as-Tool 适配器
```

---

## 核心类说明

### AgentDispatcher

路由入口，标记 `@Primary` 自动替代原来的 AgentService 注入。

```java
// 决策逻辑伪代码
if (request.multiAgent == true) → 多 Agent
if (request.multiAgent == false) → 单 Agent
else → IntentRoutingService.plan() 自动判断
```

### SubAgentRunner

复用项目已有的 Planner/Executor/Reflector/Responder 组件，但以 `SubAgentDefinition` 参数驱动而非全局 `AgentProperties`。

每个子 Agent 拥有独立的：
- 系统提示词
- 工具白名单
- 最大步数
- 反思/重规划/自检开关
- LLM 温度/token 限制

### MultiAgentState

跨 Agent 共享状态容器：

```java
state.put("loan_result", "...");                    // 子 Agent 写入
state.get("loan_result", String.class);             // 其他 Agent 读取
state.buildContextSummary();                        // Supervisor 用来做决策
state.getAllSteps();                                 // 全局执行轨迹
state.getExecutions();                              // 子 Agent 执行清单
```

**规则：** 子 Agent 只写自己的 `outputKey`，可读全部共享数据。

### SupervisorOrchestrator

核心循环：
```
for round in 1..maxRounds:
    decision = LLM决策(用户问题, 已有结果, 可用Agent列表)
    if FINISH → break
    subAgent = 查找子Agent(decision.targetAgent)
    result = SubAgentRunner.execute(subAgent)
    sharedState.put(outputKey, result)
综合回答 = LLM综合(所有sharedState)
```

---

## 配置参考

```yaml
app:
  chat:
    routing:
      multi-agent-keywords:        # 路由层触发关键词
        - 综合评估
        - 多维度分析
        - 先查再评

    multi-agent:
      enabled: true                # 总开关
      max-supervisor-rounds: 5     # Supervisor 最大循环次数
      supervisor-timeout-ms: 60000 # 总超时
      supervisor-system-prompt: |  # Supervisor 的 LLM 提示词
        ...

      agents:                      # 子 Agent 定义
        risk-agent:
          description: "风险评估专家"
          system-prompt: "..."
          allowed-tools: [rag.search, risk.assess]
          max-steps: 2
          reflect-enabled: true
          self-check-enabled: true
          temperature: 0.3
          output-key: risk_result

        loan-agent:
          description: "借款查询专家"
          allowed-tools: [loan.query, repayment.query]
          max-steps: 2
          output-key: loan_result

        rag-agent:
          description: "知识检索专家"
          allowed-tools: [rag.search]
          max-steps: 1
          output-key: rag_result

      agent-as-tool:               # Agent-as-Tool 配置
        enabled: true
        tool-name: agent.delegate
        delegatable-agents: [risk-agent, loan-agent, rag-agent]
```

---

## 请求/响应示例

### 请求（自动触发多 Agent）

```json
{
  "message": "帮我查一下 CUST001 的借款记录，再综合评估一下风险"
}
```

### 请求（显式指定多 Agent）

```json
{
  "message": "查 CUST001 借款",
  "multiAgent": true
}
```

### 响应

```json
{
  "traceId": "abc-123",
  "routeType": "MULTI_AGENT",
  "finalAnswer": "关于 CUST001 的综合评估：\n1. 借款记录：...\n2. 风险评估：...",
  "completed": true,
  "executedSteps": 12,
  "responseTimeMs": 4200,
  "steps": [
    {"stepIndex": 1, "actionType": "DELEGATE", "toolName": "supervisor", ...},
    {"stepIndex": 2, "actionType": "PLAN", ...},
    {"stepIndex": 3, "actionType": "TOOL", "toolName": "loan.query", ...},
    ...
  ],
  "subAgentExecutions": [
    {
      "agentName": "loan-agent",
      "outputKey": "loan_result",
      "response": {...},
      "latencyMs": 1800
    },
    {
      "agentName": "risk-agent",
      "outputKey": "risk_result",
      "response": {...},
      "latencyMs": 2100
    }
  ]
}
```

---

## 执行流程示例

以 "查借款记录再评估风险" 为例：

```
1. [路由] 命中 "借款记录"(tool) + "评估"(rag/risk) → MULTI_AGENT

2. [Supervisor Round 1]
   LLM: {"action":"DISPATCH", "targetAgent":"loan-agent", "reason":"先查询借款数据"}

3. [loan-agent 执行]
   Plan → loan.query(CUST001) → Reflect(done=true) → Respond
   结果写入 sharedState["loan_result"]

4. [Supervisor Round 2]
   LLM 看到 loan_result → {"action":"DISPATCH", "targetAgent":"risk-agent"}

5. [risk-agent 执行]
   读取 loan_result 作为上下文
   Plan → rag.search(风控规则) → risk.assess(CUST001) → Self-Check(pass) → Respond
   结果写入 sharedState["risk_result"]

6. [Supervisor Round 3]
   LLM: {"action":"FINISH", "reason":"信息已充足"}

7. [综合生成]
   LLM 基于 loan_result + risk_result 输出最终回答
```

---

## 如何扩展新的子 Agent

只需两步：

**1. 在 application.yml 中添加定义：**

```yaml
agents:
  collection-agent:
    description: "催收策略专家"
    system-prompt: "你是催收策略顾问..."
    allowed-tools: [loan.query, rag.search]
    max-steps: 2
    output-key: collection_result
```

**2. 如果需要新工具，实现 AgentTool 接口：**

```java
@Component
public class CollectionAdviceTool implements AgentTool {
    public String name() { return "collection.advice"; }
    public Object execute(Map<String, Object> args) { ... }
}
```

然后把 `collection.advice` 加到该 Agent 的 `allowed-tools` 列表即可。无需修改任何框架代码。

---

## 与 SuperBizAgent 的对比

| 能力 | 本项目 | SuperBizAgent |
|------|--------|---------------|
| 编排模式 | Supervisor LLM 调度 | SupervisorAgent 框架内置 |
| 子 Agent 执行 | SubAgentRunner（复用现有 Loop） | ReactAgent（框架封装） |
| 共享状态 | MultiAgentState（ConcurrentHashMap） | OverAllState（框架提供） |
| 策略约束 | AgentPolicyEnforcer + 工具白名单 | 无 |
| 可观测性 | 完整 steps 链路 + subAgentExecutions | 日志 |
| 自检降级 | Self-Check + Fallback | 无 |
| 配置化 | YAML 驱动子 Agent 定义 | 代码硬编码 |
| Agent-as-Tool | 支持 | 不支持 |

---

## 注意事项

1. **多 Agent 会增加 LLM 调用次数**：每个子 Agent 内部有 Plan + Respond 各一次 LLM 调用，Supervisor 每轮也需一次。建议对子 Agent 使用较低的 temperature 和 maxTokens。

2. **超时控制**：`supervisor-timeout-ms` 是整体超时，各子 Agent 的 `timeout-ms` 继承自全局 `AgentProperties`。

3. **工具隔离**：每个子 Agent 只能访问自己 `allowed-tools` 列表中的工具，PolicyEnforcer 的白名单检查依然生效。

4. **关闭多 Agent**：设置 `app.chat.multi-agent.enabled: false` 即可完全回退到原有单 Agent 行为。
