# AI 大模型应用开发面试问答手册

本文档专门用于面试准备，目标是帮助你把本项目讲成一个完整、清晰、有工程深度的 AI 应用实践项目。

## 1. 一分钟项目介绍

可以这样回答：

> 这个项目是我为了系统复习 AI 大模型应用开发做的一个 Spring Boot + Spring AI 实践项目。业务场景模拟金融信贷助手，覆盖普通对话、Function Calling、RAG 知识库问答、混合检索、重排序、Agent Loop、Prompt 版本管理和 AI 调用成本统计。
>
> 我重点不是简单调一个模型接口，而是练习大模型在真实后端系统中的落地方式。比如用户问题会先经过意图路由，业务查询类问题走工具调用，知识问答类问题走 RAG，复杂问题可以走 Hybrid 或 Agent 模式。Agent 模块则显式实现了 Plan、Tool、Reflect、Replan、SelfCheck、Respond 的执行闭环。
>
> 通过这个项目，我主要巩固了 Spring AI 的 ChatClient、工具调用、RAG 管道、Agent 编排、PromptOps、成本观测和工程分层设计。

## 2. 项目亮点怎么讲

面试官问“这个项目有什么亮点？”时，可以按这几个点回答：

1. **不是简单聊天机器人，而是完整 AI 应用后端。**  
   项目有 Chat、RAG API、Agent 三种入口，覆盖对话、检索、工具和编排。

2. **实现了多种大模型应用模式。**  
   包括 Spring AI Function Calling、QuestionAnswerAdvisor RAG、自研 Agent ToolRouter 和显式 Agent Loop。

3. **RAG 不只做向量检索。**  
   还实现了查询重写、查询扩展、BM25 混合检索、RRF 融合、LLM 重排序和评估指标。

4. **Agent Loop 是可观测、可回放的。**  
   每次 Agent 请求都会返回 traceId、steps、每步工具输入输出、耗时和最终答案。

5. **考虑了工程化问题。**  
   包括 Prompt 版本管理、AI 调用日志、token 成本统计、配置化路由、工具白名单和失败兜底。

## 3. 高频问题与回答模板

### Q1：你这个项目解决了什么问题？

可以回答：

> 它解决的是“如何把大模型能力落到一个真实业务系统里”的问题。普通模型只能生成文本，但企业系统需要它能查业务数据、读知识库、调用工具、控制成本、可观测和可维护。
>
> 所以我用金融信贷助手作为场景，把用户问题分成几类：如果是借款、还款、风险评估，就通过 Function Calling 调用后端数据库；如果是产品规则、风控政策，就通过 RAG 检索知识库；如果是复杂问题，就通过 Agent Loop 先规划、再调用工具、再反思和自检。

追问时可以补充：

> 这个项目更像一个 AI 应用开发知识点的综合练习，而不是单一业务系统。它帮助我理解 Chat、Tool、RAG、Agent、PromptOps 和 Observability 之间的关系。

### Q2：为什么要做意图路由？

可以回答：

> 因为不是所有问题都应该直接丢给大模型回答。业务数据查询需要查数据库，政策规则问题需要查知识库，普通闲聊才可以直接由模型回答。
>
> 项目里的 `IntentRoutingService` 会根据业务编号、关键词和配置输出 `RoutePlan`，路由类型包括 `TOOL_ONLY`、`RAG_ONLY` 和 `HYBRID`。这样可以避免模型瞎编业务数据，也能减少不必要的 RAG 和工具调用成本。

追问“为什么不用 LLM 做意图识别？”：

> 这个项目里我先用规则和关键词，是因为它更稳定、成本低、可解释，也适合学习阶段观察路由效果。生产环境可以进一步演进成规则 + 小模型分类 + LLM fallback 的组合方式。

### Q3：Function Calling 是怎么实现的？

可以回答：

> Chat 入口使用 Spring AI 的 `@Tool` 机制，把借款查询、还款查询、风险评估注册为模型可调用工具。模型根据工具描述决定是否调用，并生成结构化参数。
>
> 重构后我把真实业务逻辑下沉到了 `service.business`，比如 `LoanQueryService`、`RepaymentQueryService`、`RiskAssessmentService`。Spring AI 的 Function 类只是适配器，Agent Tool 也是适配器。这样同一份业务能力既能被 Spring AI 自动工具调用，也能被自研 Agent ToolRouter 调用。

追问“Function Calling 有什么风险？”：

> 主要风险是模型选错工具、参数不完整、返回数据太大、敏感操作不受控。所以项目里工具只做查询类操作，Agent 侧还有工具白名单，后续可以继续加参数校验、权限校验和敏感操作确认。

### Q4：RAG 流程是怎样的？

可以回答：

> 项目的 RAG 分为离线建库和在线检索两部分。
>
> 离线阶段，系统从 `src/main/resources/knowledge` 加载业务规则、产品手册、风控指南等文档，补充元数据，然后用 `TokenTextSplitter` 切分，再通过 Ollama embedding 模型生成向量，写入 `SimpleVectorStore`。
>
> 在线阶段，用户问题会先经过查询扩展或查询重写，再通过 `VectorRetrievalService` 构造统一的 `SearchRequest` 去向量库检索。检索结果会作为上下文交给模型生成回答。

追问“为什么不用数据库 LIKE 或全文索引？”：

> LIKE 更适合精确关键词，不能很好处理语义相似问题。向量检索可以找语义相关内容，比如“提前结清”和“提前还款”。但向量检索对精确编号和专业词可能弱，所以项目又加入了 BM25 和混合检索。

### Q5：查询重写和查询扩展有什么区别？

可以回答：

> 查询重写主要解决多轮对话里的上下文缺失和指代问题，比如用户问“它支持提前还款吗”，需要结合历史对话把“它”改成具体产品。
>
> 查询扩展主要解决召回率问题，比如用户说“黑名单”，系统会扩展出“征信黑名单、不良记录、失信名单”等同义词，让向量检索覆盖更多表达。

追问“查询扩展会不会引入噪声？”：

> 会，所以它适合配合 topK、相似度阈值、重排序一起使用。扩展提升召回，但可能降低精度，因此后面可以通过 rerank 或评估指标来控制效果。

### Q6：混合检索是怎么做的？

可以回答：

> 项目里混合检索结合了向量检索和 BM25。向量检索负责语义相关，BM25 负责关键词精确匹配。两路召回后使用 RRF，也就是 Reciprocal Rank Fusion，把两个排名融合起来。
>
> RRF 的好处是不需要把向量相似度和 BM25 分数强行归一化，只基于排名计算融合分数，简单稳定。

追问“什么时候会用混合检索？”：

> 包含编号、专业词、缩写、英文大写或数字的查询更适合混合检索，比如客户编号、订单号、M1 阶段、产品代码等。

### Q7：Rerank 是什么，为什么需要？

可以回答：

> Rerank 是先用向量检索召回一批候选文档，再让 LLM 对候选文档和 query 的相关性重新打分，最后按新分数排序。
>
> 向量检索速度快，适合粗召回，但排序不一定精准。LLM rerank 更慢但理解能力更强，适合对答案质量要求更高的问题。

追问“Rerank 的缺点？”：

> 缺点是成本和延迟更高，而且 LLM 输出格式可能不稳定。所以项目里做了降级处理：如果 LLM 调用失败或解析失败，就退回向量检索原始结果。

### Q8：RAG 效果怎么评估？

可以回答：

> 项目里实现了 `RagEvaluationService`，支持 Recall、Precision、F1、MRR、NDCG 等指标。
>
> Recall 看应该找到的文档找到了多少，Precision 看检索结果里有多少是真的相关，MRR 看第一个相关文档排得多靠前，NDCG 则考虑相关文档的排序位置。

追问“为什么 RAG 要评估？”：

> 因为 RAG 优化不能只靠感觉。调 chunk size、topK、阈值、扩展词、rerank 策略都会影响效果，必须用指标判断是召回率提升了，还是引入了更多噪声。

### Q9：Agent Loop 是怎么设计的？

可以回答：

> Agent 入口没有完全依赖 Spring AI 自动工具调用，而是自己实现了显式 Agent Loop。流程是 Plan、Tool、Reflect、Replan、SelfCheck、Respond。
>
> `AgentPlanner` 负责让模型输出工具调用计划，`AgentToolExecutor` 负责执行工具并处理重试和超时，`AgentReflector` 判断信息是否足够，`AgentResponder` 生成最终答案并自检，`AgentPolicyEnforcer` 负责工具白名单和风险问题强制 RAG 证据。

追问“为什么不用框架自动 Agent？”：

> 我主要是为了学习 Agent 的底层机制。自己实现一遍可以更清楚地理解规划、工具执行、观察、反思、自检和兜底，而不是把它当成黑盒。生产环境可以再考虑引入成熟 Agent 框架。

### Q10：Agent 怎么避免无限循环？

可以回答：

> 项目里有几个控制点。第一，`maxSteps` 限制最大工具调用步数；第二，`Reflect` 阶段可以判断是否已有足够信息；第三，`Replan` 有最大轮次；第四，工具调用有超时和重试次数；第五，自检失败可以走 fallback 模板，而不是继续无限调用。

### Q11：风险评估为什么要强制 RAG 证据？

可以回答：

> 风险评估属于比较敏感的业务判断，不能只依赖数据库里的用户记录，还应该参考风控规则和政策依据。
>
> 所以项目里 `AgentPolicyEnforcer` 对风险意图做了策略约束：如果识别到“风险、风控、评估、判断”等关键词，会强制加入 `rag.search`，确保最终回答里至少有知识库证据。

追问“这体现了什么设计思想？”：

> 体现了 Agent 不是完全放任模型自由决策，而是要有业务策略约束。模型负责规划，但系统要负责边界、安全和合规。

### Q12：Prompt 是怎么管理的？

可以回答：

> 项目实现了 Prompt Registry，Prompt 存在数据库中，支持草稿、发布、回滚、按环境读取。比如 system prompt、RAG QA prompt、Function Calling prompt 都可以通过版本管理。
>
> 这样做的好处是 Prompt 不硬编码在代码里，可以像配置一样治理。线上如果某个 Prompt 效果不好，可以回滚历史版本。

追问“Agent Prompt 现在怎么处理？”：

> 当前 Agent 各阶段 prompt 已拆到独立的 Planner、Reflector、Responder 服务里，后续可以进一步接入 Prompt Registry，让 Agent phase prompt 也支持版本管理。

### Q13：AI 调用成本怎么统计？

可以回答：

> 项目通过 AOP 拦截 Spring AI 的 `ChatModel.call()` 和 `EmbeddingModel.embed()`，记录模型名称、provider、耗时、状态、token 估算和成本。然后通过成本统计接口查询今日、本周、本月或指定时间范围的调用成本。

追问“为什么需要成本统计？”：

> 大模型应用和传统后端不同，每次调用都有 token 成本，而且 RAG、rerank、Agent 多步调用会放大成本。没有成本观测，就很难做优化和容量评估。

### Q14：你这个项目里 Chat、RAG API、Agent 三个入口有什么区别？

可以回答：

> Chat 入口是面向最终用户的普通对话体验，支持流式输出、历史上下文、工具调用和基础 RAG。
>
> RAG API 是面向调试和评估的检索入口，可以单独测试向量检索、混合检索、重排序和 pipeline。
>
> Agent 入口是显式智能体流程，适合复杂问题，需要先规划、调用多个工具、反思、自检再回答。

一句话总结：

> Chat 是产品入口，RAG API 是检索实验台，Agent 是复杂任务编排器。

### Q15：为什么要重构架构？

可以回答：

> 原来项目已经能跑，但 Chat、RAG API、Agent 三条链路里有一些重复逻辑，比如向量检索参数构造、工具能力调用、Agent 阶段逻辑都比较分散。
>
> 所以我做了一次面向学习和面试表达的重构：把向量检索统一到 `VectorRetrievalService`，把 Agent Loop 拆成 Planner、ToolExecutor、Reflector、Responder、Policy、State，把业务工具能力下沉到 `service.business`。这样架构更清晰，也更容易解释每一层的职责。

## 4. 深挖题：架构取舍

### 为什么保留 Spring AI Function Calling 和自研 Agent Tool 两套？

可以回答：

> 因为它们代表两种不同思路。Spring AI Function Calling 是框架自动工具调用，开发成本低，适合常规对话场景。自研 Agent Tool 是显式工具编排，能控制 Plan、Reflect、Replan、自检和兜底，适合复杂任务。
>
> 项目里保留两套，是为了对比学习。但底层业务逻辑已经统一到 `service.business`，避免重复实现。

### 为什么使用 SimpleVectorStore，而不是 Milvus、Qdrant、ES？

可以回答：

> 这个项目主要是学习和面试准备，所以选择 Spring AI 内置的 `SimpleVectorStore`，部署简单，不依赖外部向量数据库，适合本地实验。
>
> 如果上生产，我会考虑 Milvus、Qdrant、Elasticsearch 或 PGVector，重点关注数据规模、过滤能力、召回性能、运维复杂度和成本。

### 你觉得这个项目还有哪些可以继续优化？

可以回答：

> 可以从四个方向继续优化：
>
> 1. RAG 方面：增加稳定 chunk id、完善混合检索 + rerank、引入真实评测集。
> 2. Agent 方面：把各阶段 Prompt 接入 Prompt Registry，增加 Agent eval。
> 3. 工程方面：补充更多集成测试、统一异常码和日志 traceId。
> 4. 生产化方面：加入权限控制、限流、缓存、成本预算和监控告警。

## 5. 高频八股连接点

面试时可以把项目和这些知识点关联起来：

| 面试知识点 | 项目对应实现 |
|-----------|--------------|
| 大模型应用架构 | Chat / RAG / Agent 三入口 |
| Function Calling | `service.function` + Spring AI `@Tool` |
| Agent 工具调用 | `agent.tool` + `ToolRouter` |
| RAG 基础 | `VectorIndexBuilder`、`VectorRetrievalService` |
| RAG 优化 | Query Rewrite、Query Expansion、Hybrid Search、Rerank |
| Agent Loop | Planner、ToolExecutor、Reflector、Responder |
| Prompt 工程 | `PromptRegistryService` |
| 可观测性 | `AiCallLoggerAspect`、成本统计接口 |
| 工程分层 | Controller / Orchestration / Capability / Infrastructure |
| 可靠性 | 超时、重试、自检、fallback |

## 6. 面试时建议的讲解顺序

推荐按这个顺序讲，不容易乱：

1. **先讲项目定位。**  
   这是一个用于学习 AI 应用开发的金融信贷助手项目。

2. **再讲三条入口。**  
   Chat 面向用户，RAG API 面向检索调试，Agent 面向复杂任务编排。

3. **然后讲核心能力。**  
   Function Calling、RAG、Agent Loop、PromptOps、成本统计。

4. **再讲一个完整请求链路。**  
   比如“请评估 CUST1001 的风险”，系统会路由、强制 RAG、调用风险评估工具、自检后回答。

5. **最后讲优化和反思。**  
   为什么重构、还有哪些生产化优化空间。

## 7. 一个完整案例回答

面试官问：“用户问你系统：请评估 CUST1001 的风险，会发生什么？”

可以回答：

> 这个问题会先进入 Agent 入口。`IntentRoutingService` 识别到它是业务工具类问题，Agent 侧的 `AgentPolicyEnforcer` 又识别到这是风险意图，所以会强制加入 `rag.search`，确保回答有风控知识库依据。
>
> 然后 `AgentPlanner` 生成工具调用计划，通常会包含 `rag.search` 和 `risk.assess`。`AgentToolExecutor` 通过 `ToolRouter` 执行工具，`rag.search` 会走 RAG pipeline 检索风控规则，`risk.assess` 会调用 `RiskAssessmentService` 查询借款和还款记录并计算风险等级。
>
> 工具执行后，`AgentReflector` 判断信息是否足够。如果不够，会进入 Replan 补充工具调用；如果足够，`AgentResponder` 基于工具观察生成最终答案，并通过 SelfCheck 检查回答是否可靠。如果风险问题缺少 RAG 证据，自检会失败并走 fallback。
>
> 最终接口返回 finalAnswer、traceId 和完整 steps，方便观察每一步调用了什么工具、输入输出是什么、耗时多少。

## 8. 项目一句话总结

> 这是一个用 Spring Boot + Spring AI 实现的大模型应用开发综合练习项目，通过金融信贷助手场景，把 Chat、Function Calling、RAG、Agent Loop、PromptOps 和成本观测串成了一套可运行、可解释、可面试讲解的后端系统。

