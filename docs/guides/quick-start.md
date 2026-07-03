# 快速开始

本文档用于本地启动和演示当前项目。

## 1. 环境要求

| 依赖 | 版本/说明 |
|---|---|
| JDK | 17+ |
| Maven | 3.9+ |
| MySQL | 8.x |
| Milvus | 可选，启用真实 RAG 检索时需要 |
| OpenAI-compatible API | Chat 与 Embedding 模型 |

如果 Milvus 未启用，项目会使用 fallback VectorStore，应用可以正常启动，但 RAG 不会返回真实知识库证据。

## 2. 环境变量

常用环境变量：

```bash
export OPENAI_API_KEY=your_api_key
export OPENAI_BASE_URL=https://api.openai.com
export CHAT_MODEL=gpt-4o-mini
export EMBEDDING_MODEL=text-embedding-3-small

export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_DATABASE=aidevelop
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=your_password
```

如果使用 Milvus：

```bash
export MILVUS_ENABLED=true
export MILVUS_HOST=localhost
export MILVUS_PORT=19530
export MILVUS_COLLECTION=financial_knowledge
```

## 3. 数据库初始化

按需执行 SQL：

```text
sql/demo_tables.sql
sql/chat_memory.sql
sql/prompt_registry.sql
sql/ai_cost_tracking.sql
sql/agent_trace.sql
```

Docker 初始化脚本会自动包含上述核心表。

## 4. 启动

```bash
mvn spring-boot:run
```

启动后访问：

| 页面 | 地址 |
|---|---|
| 聊天工作台 | `http://localhost:8080/` |
| Prompt 管理 | `http://localhost:8080/prompt.html` |
| 成本看板 | `http://localhost:8080/cost.html` |
| 健康检查 | `http://localhost:8080/health` |

## 5. 核心接口

```bash
# 常规聊天
curl -X POST http://localhost:8080/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"解释一下什么是 RAG","mode":"general"}'

# 金融 RAG
curl -X POST http://localhost:8080/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"提前还款有什么规则？","mode":"financial_rag"}'

# Agent 任务
curl -X POST http://localhost:8080/api/agent/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"查询 CUST1001 的借款和还款情况，并结合规则评估风险"}'

# 查询 Agent trace
curl http://localhost:8080/api/agent/trace/{traceId}
```

说明：生产接口中没有独立 RAG 调试入口。RAG 由 `RagFacade` 在 Chat 和 Agent 内部统一调用。

## 6. 推荐演示路径

1. 打开首页，先用 `general` 问一个通用技术问题，展示普通 Chat。
2. 切换到 `financial_rag`，询问金融规则，展示知识库证据注入。
3. 使用 Agent 任务查询 `CUST1001`，展示工具调用、反思、最终回答。
4. 使用复杂问题触发多 Agent，展示 Supervisor 调度和子 Agent 输出。
5. 打开成本看板，展示 LLM 调用日志和 token/cost 统计。
6. 根据 `traceId` 查询 Agent trace，展示执行步骤回放。

## 7. 目录速览

```text
src/main/java/com/example/aidevelop/
├── controller/           # Chat、Prompt、Cost、Health API
├── agent/                # Agent Runtime、多 Agent、工具
├── service/rag/          # RagFacade 与 RAG Pipeline
├── service/prompt/       # Prompt Registry
├── service/cost/         # 成本统计
├── model/entity/         # JPA 实体
├── repository/           # Spring Data JPA
└── interceptor/          # AiCallLoggerAspect
```
