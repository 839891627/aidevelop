# AI Chat Assistant - 智能对话助手系统

Java 开发者学习 AI Agent 开发的实战项目 | 基于 Spring Boot 3.3 + Spring AI 构建

---

## 项目简介

这是一个循序渐进的 **AI Agent 学习项目**，从基础的 AI 对话到高级的 RAG 检索增强生成，覆盖了 Java 开发者转型 AI 方向的核心知识点。每个功能模块都是独立的，可以逐步学习和实践。

## 已实现的功能

| 模块 | 功能 | 说明 |
|------|------|------|
| **基础对话** | 多 LLM 支持 | OpenAI 兼容模型（默认 deepseek-chat） |
| | 流式响应 | SSE 实时打字机效果 |
| | 对话历史 | 滑动窗口管理，控制上下文长度 |
| **Function Calling** | 贷款查询 | AI 自动调用后端函数查询贷款信息 |
| | 还款查询 | 查询还款记录 |
| | 风险评估 | 基于规则的风险评估函数 |
| **RAG 检索增强** | 向量检索 | 基于语义相似度的知识库检索 |
| | 查询扩展 | 同义词 + 专业术语扩展提升召回率 |
| | 查询重写 | 指代消解和上下文补全 |
| | 混合检索 | 向量检索 + BM25 关键词检索，RRF 融合 |
| | LLM 重排序 | 先召回后精排，提升精确率 |
| | RAG 管道 | 自动组合以上策略的智能检索 |
| | RAG 评估 | 召回率、精确率、F1、MRR、NDCG 指标 |
| **成本管理** | 调用日志 | AOP 自动记录每次 AI 调用 |
| | 成本统计 | 按日/周/月统计 Token 和费用 |
| | 前端看板 | 可视化成本管理界面 |
| **Prompt 管理** | 模板管理 | 外部化 Prompt 配置和版本管理 |

## 技术栈

- **后端**: Spring Boot 3.3.5, Spring AI 1.0.0-M5, Java 17
- **前端**: HTML5, CSS3, JavaScript (原生)
- **数据库**: MySQL (JPA + Hibernate)
- **构建工具**: Maven 3.9+

### 依赖稳定性策略

- Spring AI 当前锁定为 `1.0.0-M5`（与现有代码 API 保持兼容）。
- Maven 仓库仅保留 `spring-milestones`，移除 `snapshot` 仓库，避免拉取不稳定快照包。
- 后续升级到 Spring AI 1.0 GA 建议单独开迁移分支处理 API 变更。

---

## 快速开始

### 前提条件

- Java 17+
- Maven 3.6+
- MySQL 数据库
- AI 模型 API Key（智谱 AI / OpenAI 兼容）

### 启动步骤

1. **配置环境变量**（或创建 `.env` 文件参考 `.env.example`）

```bash
# 数据库
export DB_URL=jdbc:mysql://localhost:3306/ai_develop
export DB_USERNAME=root
export DB_PASSWORD=your-password

# AI 模型（对话模型）
export OPENAI_API_KEY=your-openai-compatible-key
export OPENAI_BASE_URL=https://api.deepseek.com
export OPENAI_CHAT_MODEL=deepseek-chat
```

2. **启动应用**

```bash
mvn spring-boot:run
```

3. **访问页面**

| 页面 | 地址 |
|------|------|
| 聊天界面 | http://localhost:8080/index.html |
| 成本管理 | http://localhost:8080/index.html |
| Swagger 文档 | http://localhost:8080/swagger-ui.html |
| 健康检查 | http://localhost:8080/health |

---

## 项目结构

```
src/main/java/com/example/aidevelop/
├── config/                     # 配置类
│   ├── AiModelConfig.java      # AI 模型配置
│   ├── CacheConfig.java        # 缓存配置
│   ├── PromptProperties.java   # Prompt 模板配置
│   ├── RagProperties.java      # RAG 参数配置
│   ├── VectorStoreConfig.java  # 向量库配置
│   └── SwaggerConfig.java      # Swagger 配置
├── controller/                 # REST 控制器
│   ├── ChatController.java     # 聊天 + RAG 检索 API
│   ├── AiCostController.java   # 成本统计 API
│   ├── ModelController.java    # 模型信息 API
│   ├── PromptController.java   # Prompt 管理 API
│   └── HealthController.java   # 健康检查
├── model/
│   ├── dto/                    # 数据传输对象
│   │   ├── chat/               # 聊天相关 DTO
│   │   └── rag/                # RAG 检索相关 DTO
│   └── entity/                 # JPA 实体
├── repository/                 # 数据访问层
├── service/
│   ├── ChatService.java        # 聊天服务接口
│   ├── impl/ChatServiceImpl.java
│   ├── cost/                   # 成本计算和统计
│   ├── function/               # Function Calling 函数
│   ├── prompt/                 # Prompt 模板管理
│   ├── cache/                  # 缓存服务
│   └── rag/                    # RAG 检索相关服务
│       ├── QueryExpansionService.java
│       ├── QueryRewriteService.java
│       ├── BM25Service.java
│       ├── HybridSearchService.java
│       ├── RerankService.java
│       ├── RagPipelineService.java
│       └── RagEvaluationService.java
├── interceptor/                # AOP 切面（调用日志）
├── scheduled/                  # 定时任务
└── exception/                  # 异常处理
```

---

## 核心 API

### 聊天

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat` | 普通聊天（阻塞式） |
| POST | `/api/chat/stream` | 流式聊天（SSE） |
| DELETE | `/api/chat/{conversationId}` | 清空对话历史 |

### RAG 检索（高级管线）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/rag/search?query=xxx` | 向量检索 |
| GET | `/api/rag/hybrid-search?query=xxx` | 混合检索（向量+BM25） |
| GET | `/api/rag/rerank-search?query=xxx` | 重排序检索 |
| GET | `/api/rag/pipeline?query=xxx` | 智能 RAG 管道 |
| POST | `/api/rag/evaluate` | RAG 系统评估 |

### Chat 链路与 RAG 链路说明

- `/api/chat`：对话主链路，使用 `ChatClient`，并可通过配置启用内置 `QuestionAnswerAdvisor`（基础 RAG）。
- `/api/rag`：高级 RAG 实验链路，提供查询扩展、查询重写、混合检索、重排与评估等独立能力。
- 建议学习顺序：先掌握 `/api/chat`，再深入 `/api/rag`。

### 对话历史持久化边界

- 当前 `ConversationRepository` 为内存实现（`ConcurrentHashMap`），适合教学和本地开发。
- 应用重启后会话历史会丢失，不属于生产持久化方案。
- 若要用于生产，建议替换为 Redis / MySQL 等持久化存储。

### 调试接口（仅 `dev` Profile）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/chat/debug/vector-store` | 向量库状态概览 |
| GET | `/api/chat/debug/test-search?query=xxx` | 检索分数调试（可选内容预览） |
| GET | `/api/chat/debug/query-expansion?query=xxx` | 查询扩展调试 |
| GET | `/api/chat/debug/query-rewrite?query=xxx` | 查询重写调试 |

### 成本统计

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/cost/today` | 今日成本 |
| GET | `/api/cost/week` | 本周成本 |
| GET | `/api/cost/month` | 本月成本 |
| GET | `/api/cost/range?start=&end=` | 自定义时间范围 |

---

## 学习文档

所有学习文档统一放在 `docs/` 目录，按难度递进排列：

| 文档 | 主题 | 难度 |
|------|------|------|
| [01-quick-start](docs/01-quick-start.md) | 项目架构总览与快速开始 | ★ |
| [02-chat-basics](docs/02-chat-basics.md) | 基础对话、流式响应、对话历史 | ★ |
| [03-multi-llm](docs/03-multi-llm.md) | 多 LLM 接入、Profile 切换 | ★★ |
| [04-prompt-engineering](docs/04-prompt-engineering.md) | Prompt 模板管理、系统提示词 | ★★ |
| [05-function-calling](docs/05-function-calling.md) | AI 函数调用、工具使用 | ★★★ |
| [06-rag-basics](docs/06-rag-basics.md) | RAG 基础、向量检索、知识库 | ★★★ |
| [07-rag-advanced](docs/07-rag-advanced.md) | 查询重写、混合检索、重排序、管道 | ★★★ |
| [08-cost-and-observability](docs/08-cost-and-observability.md) | 成本管理、AOP 日志、缓存 | ★★ |
| [10-agent-loop-design](docs/10-agent-loop-design.md) | 从 Chat+RAG 升级到 Agent Loop 的实施设计 | ★★★★ |
| [AI_LEARNING_PATH](docs/AI_LEARNING_PATH.md) | 4 周学习路线图 | - |

详见 [docs/README.md](docs/README.md)。

---

## 许可证

MIT License
