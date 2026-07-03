# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

金融信贷场景下的 Spring Boot 3.3.5 + Spring AI 1.1.6 AI 应用工程化样板（Java 17）。核心能力：Chat（三种模式）、统一 RAG、Agent Runtime、多 Agent Supervisor、Prompt 治理、成本观测与 Agent Trace 持久化。能力矩阵、接口列表、目录结构和 SQL 脚本见 `README.md`，不再在此重复。

## 常用命令

项目未包含 `mvnw`，直接使用系统 `mvn`：

- 构建：`mvn clean package`
- 运行：`mvn spring-boot:run`（默认端口 8080，首页 `http://localhost:8080/`）
- 全量测试：`mvn test`
- 单个测试类：`mvn test -Dtest=AgentLoopServiceTest`
- 单个测试方法：`mvn test -Dtest=AgentLoopServiceTest#shouldReturnAgentResponse`

**测试全部是纯单元测试**（Mockito + `MockMvcBuilders.standaloneSetup`），不启动 Spring 上下文、不连数据库、不调用 LLM，因此无任何外部依赖即可快速运行。新增测试请保持这一约定，不要引入 `@SpringBootTest`。

## 运行环境

- 默认激活 `openai` profile（`application.yml` 中 `spring.profiles.active: openai`）。`AiModelConfig.chatClientForOpenAI` 仅在该 profile 下创建 `@Primary` 的 `ChatClient` Bean，Chat 和 Agent 链路均通过 `@Resource(name = "chatClientForOpenAI")` 注入它，缺少该 Bean 两条链路都无法工作。
- 配置通过环境变量注入（参考 `.env.example`），关键变量：`OPENAI_API_KEY`、`OPENAI_BASE_URL`、`OPENAI_CHAT_MODEL`、`EMBEDDING_MODEL`、`DB_URL`/`DB_USERNAME`/`DB_PASSWORD`。OpenAI-compatible 路径/模型在 `application-openai.yml`。
- MySQL 必需；Milvus 可选——`MILVUS_ENABLED=false` 时 `FallbackVectorStoreConfig` 提供空结果降级 VectorStore，应用能启动但 RAG 不返回真实证据。
- 基础设施可用 `docker-compose up -d`（MySQL / Milvus / etcd / Attu）。
- `hibernate.ddl-auto=none`，不自动建表，需手动执行 `sql/*.sql`（docker 初始化脚本会自动包含核心表）。

## 核心架构

### 三条 Chat 链路与意图路由

`ChatMode`（`general` / `financial_rag` / `auto`）决定 `ChatServiceImpl` 走哪条链路：

- `general`：不路由，纯聊天，不挂 RAG / 工具。
- `financial_rag`：固定 RAG_ONLY，强制 `RagFacade` 注入知识库证据。
- `auto`：交由 `IntentRoutingService.plan(message)` 输出 `RoutePlan`（`TOOL_ONLY` / `RAG_ONLY` / `HYBRID` / `MULTI_AGENT`）。

`IntentRoutingService` 是可运营路由核心：用 `app.chat.routing.*` 下的关键词和正则（业务编号 `LOAN\d+`/`CUST\d+` 等）做规则判断，`resolveAllowedToolNames` 取“路由希望开放”与“系统实际启用（`app.tools.enabled`）”的交集。`RoutePlan` 同时被 Chat 和 Agent 复用——改路由行为优先调配置而非改代码。

### 统一 RAG 入口

`RagFacade` 是 Chat 和 Agent 唯一的 RAG 检索入口，避免两套链路行为分叉：

- Chat：`ChatServiceImpl -> RagFacade.retrieve(RagProfile.CHAT) -> RagContextFormatter` 注入 prompt。
- Agent：`RagSearchAgentTool -> RagFacade.retrieve(RagProfile.AGENT)` 作为 `rag.search` observation。

`RagFacade` 委托 `RagPipelineService`（查询重写 → 查询扩展 → 策略选择 → 向量/混合/rerank 检索），失败时返回降级 `RagRetrievalResult` 而非抛异常。生产链路不暴露独立 RAG 调试接口。

### Agent Runtime（单 Agent）

`AgentLoopService` 编排闭环：Plan → Tool → Observe → Reflect →（Replan → …）→ Respond → SelfCheck，每一步记录为 `AgentStep`（含 `AgentStepStatus` 和 `AgentFailureReason`）。关键工程化点：

- **结构化输出**：Planner / Reflector / Responder / Supervisor 不各自解析 JSON，统一走 `AgentStructuredOutputValidator`，失败归类为 `AgentStructuredOutputException` 并写入 step。
- **预算、超时与限流**：`AgentBudgetTracker` 限单请求 round / LLM 调用 / tool 调用；`AgentLlmClient` 用 `CompletableFuture.orTimeout` 控单次 LLM 超时，超时映射为 `AgentFailureReason.LLM_TIMEOUT`，预算超限映射为 `BUDGET_EXCEEDED`；`AgentRateLimiter`（默认 `TokenBucketRateLimiter` 单机令牌桶）在 `AgentLlmClient.call()` 和 `AgentToolExecutor.executeWithRetry()` 拦截，LLM 全局 + 工具按名分桶（QPS 令牌桶 + 并发 Semaphore），超时映射为 `RATE_LIMITED`。三者都走兜底回答而非抛给调用方。
- **trace 上下文**：`AgentTraceContext`（ThreadLocal）携带 `traceId` / `phase` / `budgetTracker`，`finally` 中必须 `clear()`。它同时被 `AiCallLoggerAspect` 读取，把每条 `ai_call_log` 关联到 agent trace。
- **持久化**：`AgentTraceService.persistTrace` best-effort 写 `agent_trace` / `agent_step`，失败不影响主响应。

### 多 Agent

`AgentDispatcher` 是 `@Primary` 的 `AgentService`，根据 `MultiAgentProperties.enabled`、请求 `multiAgent` 标志和路由结果，分流到单 Agent（`AgentLoopService`）或多 Agent（`SupervisorOrchestrator`）。Supervisor 每轮让 LLM 输出结构化决策（`DISPATCH`/`FINISH`）调度子 Agent；子 Agent 通过 `SubAgentRunner` **复用单 Agent Runtime**，因此自动继承结构化校验 / 超时 / 预算 / trace。`MultiAgentState` 是共享黑板（`loan_result`/`rag_result`/`risk_result`）。工具隔离两层：`SubAgentDefinition.allowedTools` + `ToolRouter.isAllowed()` 全局白名单兜底。另支持 Agent-as-Tool（`agent.delegate` → `SubAgentTool`）。

### 两套工具系统

同一份业务逻辑（`service.business`）有两个适配层，**不要把业务逻辑写进适配层**，否则 Chat 和 Agent 链路会再次分叉：

- Spring AI `@Tool`（`service.function`，实现 `AiToolProvider`）：服务 Chat 的 Function Calling，由 `AiModelConfig` 收集，按 `RoutePlan.allowedToolNames` 通过 `toolNames(...)` 动态暴露。
- Agent Tool（`agent.tool`，实现 `AgentTool`，由 `ToolRouter` 路由）：服务 Agent Loop 的显式工具执行，可做白名单 / 重试 / 超时 / trace。`ToolRouter` 延迟构建注册表，并用 `AgentProperties.allowedTools` 做全局白名单兜底。

新增工具推荐顺序（详见 `docs/guides/function-calling.md`）：业务服务 → `@Tool` 适配（如需 Chat）→ `AgentTool` 适配并注册 `ToolRouter` / `allowedTools`（如需 Agent）→ 路由白名单。

### 成本观测与 Prompt 治理

- `AiCallLoggerAspect`（AOP `@Around` 拦截 `ChatModel.call` / `EmbeddingModel.embed`）记录 token / 耗时 / 成本到 `ai_call_log`，通过 `AgentTraceContext.traceId` 与 agent 链路关联。注意它对 token 做了估算，并非全部来自响应 metadata。
- `PromptRegistryService` 提供 DB 版本化 prompt（草稿 / 发布 / 回滚），DB 未初始化时使用内置兜底 prompt。Chat 三种模式分别用 `chat.general` / `chat.financial.rag` / `system.default`。

## 配置

可调参数集中在 `application.yml` 的 `app.*` 下：`app.chat`（含 `rag` / `routing` / `agent` / `multi-agent`）、`app.prompts`、`app.tools`、`app.cost-tracking`，对应 `config/` 下的 `@ConfigurationProperties` 类（`AgentProperties`、`RouteProperties`、`RagProperties`、`MultiAgentProperties` 等）。

## 约定

- 代码注释与文档以中文为主，新增代码请保持同一风格。
- `.env`、`.claude/`、`logs/` 已在 `.gitignore` 中，不要提交。
- 详细架构与使用文档在 `docs/`（入口 `docs/README.md`），面试讲解见 `docs/architecture/interview-guide.md`。
- 当前默认分支为 `ai-app-refactor`，主分支为 `main`。
