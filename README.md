# AI Chat Assistant - 智能对话助手系统

Java 开发者学习 Spring AI、RAG、Function Calling 和 Agent Loop 的实战项目。当前项目定位为**本地学习与演示**，不是开箱即用的生产系统。

---

## 项目简介

本项目基于 Spring Boot + Spring AI 构建，从普通聊天开始，逐步加入会话记忆、工具调用、知识库检索、RAG 评估、成本统计、Prompt Registry、Agent Loop 和多 Agent 协作。代码尽量保持模块化，方便按章节阅读、调试和扩展。

## 当前能力

| 模块 | 已实现能力 | 代码入口 |
|------|------------|----------|
| 基础对话 | 阻塞式聊天、SSE 流式聊天、显式 `general` / `financial_rag` / `auto` 能力模式、运行时覆盖模型/温度/maxTokens | `ChatController`, `ChatServiceImpl`, `ChatMode` |
| 会话记忆 | 基于 MySQL `chat_message` 表持久化多轮对话，支持按 `conversationId` 清空 | `ConversationRepository` |
| 意图路由 | 根据关键词和业务编号判断工具/RAG/混合链路 | `IntentRoutingService` |
| Function Calling | 贷款查询、还款查询、风险评估工具 | `service/function`, `agent/tool` |
| RAG | 向量检索、BM25、混合检索、查询扩展、查询重写、LLM 重排、管道检索 | `RagController`, `service/rag` |
| 向量库 | 启动时从 `src/main/resources/knowledge/*.txt,pdf` 构建 `SimpleVectorStore`，并持久化到本地文件 | `VectorStoreConfig`, `VectorIndexBuilder` |
| Agent Loop | Plan、Tool、Reflect、Replan、SelfCheck、Respond，返回 `traceId` 和步骤明细 | `AgentController`, `AgentLoopService` |
| 多 Agent 协作 | Supervisor 编排多子 Agent、Agent-as-Tool 委托、跨领域复杂问题自动调度 | `SupervisorOrchestrator`, `SubAgentRunner`, `AgentDispatcher` |
| Prompt Registry | Prompt 查询、草稿、发布、回滚、版本列表，支持 `chat.general` 与 `chat.financial.rag` 等分场景提示词 | `PromptController`, `PromptRegistryService` |
| 成本统计 | AOP 记录 AI 调用日志，按今日/本周/本月/时间范围统计 | `AiCallLoggerAspect`, `AiCostController` |
| 前端演示 | 原生 HTML/CSS/JS 聊天工作台、Prompt 管理页和成本看板，聊天页显式展示常规/RAG/Agent 能力模式 | `src/main/resources/static` |

## 技术栈

- Java 17
- Spring Boot 3.3.5
- Spring AI 1.1.6
- Spring Web MVC, Spring Data JPA, Spring AOP, Validation
- OpenAI 兼容 Chat Model（默认示例为 DeepSeek）
- 火山方舟 doubao-embedding-vision（Embedding Model）
- MySQL
- Maven
- Knife4j / Swagger UI

---

## 快速开始

### 前提条件

- JDK 17+
- Maven 3.6+
- Docker & Docker Compose（推荐，一键启动 MySQL + Milvus）
- 一个 OpenAI 兼容接口的 API Key（聊天 + Embedding 均走火山方舟）

### 1. 启动基础设施

```bash
docker-compose up -d
```

MySQL 和 Milvus 首次启动时会自动完成初始化：
- MySQL 自动执行 `sql/` 下的建表脚本（通过 `docker-init/01-init.sh`）
- Milvus 开箱即用，无需额外配置

如果不使用 Docker，手动导入 SQL：

```bash
mysql -u root -p ai_develop < sql/demo_tables.sql
mysql -u root -p ai_develop < sql/ai_cost_tracking.sql
mysql -u root -p ai_develop < sql/chat_memory.sql
mysql -u root -p ai_develop < sql/prompt_registry.sql
```

### 2. 配置环境变量

不要把真实密钥提交到 Git。可以在本机 Shell、IDE Run Configuration 或未提交的 `.env` 中配置：

```bash
# MySQL
export DB_URL=jdbc:mysql://localhost:3306/ai_develop
export DB_USERNAME=root
export DB_PASSWORD=your-password

# Chat & Embedding: 火山方舟 OpenAI 兼容 API
export OPENAI_API_KEY=your-ark-api-key
export OPENAI_BASE_URL=https://ark.cn-beijing.volces.com/api/plan/v3
export OPENAI_CHAT_COMPLETIONS_PATH=/chat/completions
export OPENAI_EMBEDDINGS_PATH=/embeddings
export OPENAI_CHAT_MODEL=your-chat-endpoint-id
export EMBEDDING_MODEL=doubao-embedding-vision
export MILVUS_EMBEDDING_DIMENSION=2048
export RAG_EMBEDDING_BATCH_SIZE=10
```

### 3. 启动应用

```bash
mvn spring-boot:run
```

首次启动时，应用会在启动完成后异步读取 `src/main/resources/knowledge/` 下的 TXT/PDF 文档，切分后写入 Milvus。方舟 Agent Plan 的 Embeddings API 单次 `input` 上限为 10，项目默认通过 `RAG_EMBEDDING_BATCH_SIZE=10` 分批写入。

### 4. 访问入口

| 入口 | 地址 |
|------|------|
| 聊天界面 | http://localhost:8080/index.html |
| Prompt 管理 | http://localhost:8080/prompt.html |
| 成本看板 | http://localhost:8080/cost.html |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Knife4j | http://localhost:8080/doc.html |
| 健康检查 | http://localhost:8080/health |

---

## 核心 API

### Chat

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat` | 普通聊天，返回完整响应 |
| POST | `/api/chat/stream` | SSE 流式聊天 |
| DELETE | `/api/chat/{conversationId}` | 清空指定会话历史 |

`/api/chat` 和 `/api/chat/stream` 支持 `ChatRequest.mode`：

| mode | 说明 |
|------|------|
| `general` | 常规聊天，使用 `chat.general`，不启用金融 RAG 和工具 |
| `financial_rag` | 金融助贷知识库问答，使用 `chat.financial.rag` 并挂载 `QuestionAnswerAdvisor` |
| `auto` | 兼容旧自动路由，使用 `system.default` 并经过 `IntentRoutingService` |

前端聊天页默认使用 `general`，金融知识库问题可选择“金融 RAG”，需要查询业务数据或执行风险评估时选择“Agent 任务”。

### Agent Loop

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/agent/chat` | 执行 Agent Loop，返回最终答案、`traceId`、路由类型和步骤明细 |

Agent 默认最多执行 3 个工具步骤，并支持：

- Planner 生成工具调用计划
- 工具执行失败重试
- Reflect 判断信息是否足够
- Replan 补充工具调用
- SelfCheck 给最终答案打分
- 风险评估类问题强制要求 RAG 证据，不满足时走稳健回退回答

当检测到跨领域复杂问题时，系统自动切换为多 Agent 协作模式：

- **Supervisor 编排**：Supervisor LLM 分析用户意图，决定调度哪些子 Agent、以什么顺序执行
- **Agent-as-Tool**：子 Agent 作为工具被 Supervisor 调用，支持配置可委托的 Agent 列表
- **子 Agent 独立执行**：每个子 Agent 拥有独立的 System Prompt、工具白名单、步数限制和温度参数
- **结果汇总**：Supervisor 汇总各子 Agent 执行结果，生成最终回答

### RAG 检索

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/rag/search?query=xxx&type=规则&topK=5` | 基础向量检索，可按文档类型过滤 |
| GET | `/api/rag/hybrid-search?query=xxx&topK=5` | 向量检索 + BM25，使用 RRF 融合 |
| GET | `/api/rag/rerank-search?query=xxx&topK=5` | 向量召回后使用 LLM 重排序 |
| GET | `/api/rag/pipeline?query=xxx&conversationId=xxx&topK=5` | 查询重写、扩展、检索、重排的组合管道 |
| POST | `/api/rag/evaluate` | 计算 Recall、Precision、F1、MRR、NDCG 等指标 |

### Prompt Registry

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/prompts/system` | 查看兼容链路 System Prompt |
| GET | `/api/prompts/rag/qa` | 查看 RAG QA Prompt |
| GET | `/api/prompts/function/calling` | 查看 Function Calling Prompt |
| GET | `/api/prompts/status` | 查看 Prompt 状态摘要 |
| POST | `/api/prompts/registry/drafts` | 创建草稿版本 |
| POST | `/api/prompts/registry/publish` | 发布版本为 ACTIVE |
| POST | `/api/prompts/registry/rollback` | 回滚到指定版本 |
| GET | `/api/prompts/registry/active` | 查询当前生效版本 |
| GET | `/api/prompts/registry/versions` | 查询版本列表 |

当前核心 prompt key：

| promptKey | 用途 |
|-----------|------|
| `chat.general` | 常规聊天中立提示词 |
| `chat.financial.rag` | 金融 RAG 场景提示词 |
| `system.default` | 旧自动路由/兼容链路提示词 |
| `rag.qa` | RAG QA 模板 |
| `function.calling` | Function Calling 模板 |

### 调试与成本统计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/chat/debug/vector-store` | 向量库状态概览 |
| GET | `/api/chat/debug/test-search?query=xxx&includeContent=false` | 检索分数调试，可选返回内容片段 |
| GET | `/api/chat/debug/query-expansion?query=xxx` | 查询扩展调试 |
| GET | `/api/chat/debug/query-rewrite?query=xxx&conversationId=xxx` | 查询重写调试 |
| GET | `/api/cost/today` | 今日成本 |
| GET | `/api/cost/week` | 本周成本 |
| GET | `/api/cost/month` | 本月成本 |
| GET | `/api/cost/range?start=2026-05-01T00:00:00&end=2026-05-12T23:59:59` | 自定义时间范围成本 |

---

## 关键配置

主要配置集中在 `src/main/resources/application.yml` 和 `application-openai.yml`。

| 配置段 | 说明 |
|--------|------|
| `spring.profiles.active=openai` | 默认启用 OpenAI 兼容聊天模型配置 |
| `spring.ai.openai.*` | Chat Model 和 Embedding Model 的 API Key、Base URL、路径和模型名（均走火山方舟） |
| `spring.ai.vectorstore.milvus.*` | Milvus 连接、集合名、向量维度和索引参数 |
| `app.chat.rag.*` | RAG 开关、相似度阈值、topK、分块、Embedding 分批大小和管道参数 |
| `app.chat.routing.*` | 意图路由关键词、工具白名单、混合链路参数 |
| `app.chat.agent.*` | Agent 步数、超时、重试、Reflect/Replan/SelfCheck 配置 |
| `app.chat.multi-agent.*` | 多 Agent 协作开关、Supervisor 轮数/超时、子 Agent 定义、Agent-as-Tool 配置 |
| `app.prompts.*` | Prompt Registry 开关和环境 |
| `app.cost-tracking.*` | AI 调用日志和成本统计开关 |

---

## 项目结构

```text
src/main/java/com/example/aidevelop/
├── agent/                      # Agent 模块
│   ├── controller/             # Agent API 入口
│   ├── model/                  # Agent 请求/响应模型
│   ├── multi/                  # 多 Agent 协作：Supervisor 编排、子 Agent 运行、Agent-as-Tool
│   ├── service/                # Agent Loop 核心服务（Plan/Reflect/Replan/SelfCheck）
│   └── tool/                   # Agent 工具定义与路由
├── config/                     # Spring AI、RAG、向量库、Swagger、CORS 等配置
├── controller/                 # Chat、RAG、Prompt、成本、调试、健康检查 API
├── exception/                  # 统一异常处理
├── interceptor/                # AI 调用日志 AOP
├── model/                      # DTO 和 JPA Entity
├── repository/                 # 会话、成本、Prompt 等持久化接口
├── scheduled/                  # 定时任务（每日成本统计）
└── service/                    # Chat、路由、工具函数、RAG、Prompt、成本统计

src/main/resources/
├── application.yml             # 主配置
├── application-openai.yml      # OpenAI 兼容模型配置
├── knowledge/                  # RAG 知识库 TXT/PDF
├── static/                     # 原生前端页面
└── knife4j/                    # Knife4j 文档补充

sql/                            # 演示库表和初始化数据
docs/                           # 学习文档和设计文档
```

---

## 安全与开源说明

当前代码适合本地学习，不建议直接作为公网服务部署。开源或部署前至少处理以下事项：

- 不要提交 `.env`、真实 API Key、数据库密码、日志、IDE 配置和本地向量库文件。
- `CorsConfig` 当前允许任意来源、方法和 Header，并允许 credentials；生产环境应改为明确来源白名单。
- 当前 Controller 未内置统一鉴权；Prompt 管理、调试、RAG 内容预览、成本统计等接口部署到公网前必须加认证和授权。
- `application.yml` 默认开启 Swagger/Knife4j，并包含较多 DEBUG/TRACE 日志；生产环境应关闭调试文档或增加访问控制，并降低日志级别。
- `src/main/resources/knowledge/` 和其他样例资料应确认均为可公开的虚构数据；如包含真实个人、客户、公司或业务资料，需要先脱敏或替换。
- 向量库文件可能保留源文档语义信息，建议不要提交 `data/vector-store*.json`，由使用者本地重新生成。

---

## 学习文档

所有学习文档统一放在 `docs/` 目录。推荐先读 [architecture](docs/architecture.md) 建立全局地图，再读 [case-execution-flows](docs/11-case-execution-flows.md) 用具体问题串代码链路，之后按 `01` 到 `10` 逐步阅读；如果只做面试复盘，优先看 `architecture`、`case-execution-flows`、`chat-basics`、`prompt-engineering`、`rag-basics`、`design/agent-loop` 和 `cost-and-observability`。

| 文档 | 主题 | 难度 |
|------|------|------|
| [architecture](docs/architecture.md) | 项目核心架构说明（前端模式、Chat 路由、Prompt/RAG/Agent/成本图解） | 架构 |
| [01-quick-start](docs/01-quick-start.md) | 项目架构总览与快速开始 | ★ |
| [02-chat-basics](docs/02-chat-basics.md) | 基础对话、流式响应、对话历史 | ★ |
| [03-multi-llm](docs/03-multi-llm.md) | 多 LLM 接入、Profile 切换 | ★★ |
| [04-prompt-engineering](docs/04-prompt-engineering.md) | Prompt 模板管理、系统提示词 | ★★ |
| [05-function-calling](docs/05-function-calling.md) | AI 函数调用、工具使用 | ★★★ |
| [06-rag-basics](docs/06-rag-basics.md) | RAG 基础、向量检索、知识库 | ★★★ |
| [07-rag-advanced](docs/07-rag-advanced.md) | 查询重写、混合检索、重排序、管道 | ★★★ |
| [08-cost-and-observability](docs/08-cost-and-observability.md) | 成本管理、AOP 日志、缓存 | ★★ |
| [09-embedding-and-chunking](docs/09-embedding-and-chunking.md) | Embedding 与分块策略 | ★★★ |
| [10-chat-memory](docs/10-chat-memory.md) | Chat Memory 持久化与流式会话续聊 | ★★★ |
| [11-case-execution-flows](docs/11-case-execution-flows.md) | 常规聊天、金融 RAG、Agent、Prompt、成本等 case 链路执行说明 | ★★ |
| [multi-agent-architecture](docs/multi-agent-architecture.md) | 多 Agent 协作架构文档 | 架构 |
| [interview-guide](docs/interview-guide.md) | AI 大模型应用开发面试问答手册 | 面试 |
| [design/agent-loop](docs/design/agent-loop.md) | Agent Loop 设计文档 | 设计稿 |
| [design/enterprise-ai-evolution-todo](docs/design/enterprise-ai-evolution-todo.md) | 企业级 AI 系统演进 TODO | 设计稿 |

详见 [docs/README.md](docs/README.md)。

---

## 许可证

MIT License
