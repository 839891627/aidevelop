# 01 - 项目快速开始与架构总览

## 1. 项目简介

这是一个**金融助贷领域的 AI 智能助手**，专为 Java 开发者学习 AI Agent 开发而设计。项目从一个简单的聊天功能出发，逐步扩展到 RAG 检索增强、Function Calling、成本管理等 AI 应用核心能力。

### 业务场景

模拟一个金融助贷系统的智能客服，能够：
- 回答贷款产品、业务规则等知识性问题（RAG）
- 查询具体贷款和还款数据（Function Calling）
- 评估客户风险等级（Function Calling + 业务逻辑）
- 统计 AI 调用成本（可观测性）

## 2. 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.5 | 应用框架 |
| Spring AI | 1.0.0-M5 | AI 模型抽象层 |
| Java | 17 | 开发语言 |
| Spring WebFlux | - | SSE 流式响应 |
| Spring Data JPA | - | 数据持久化 |
| MySQL | 8.x | 数据库 |
| Caffeine | - | 本地缓存 |
| SpringDoc OpenAPI | - | Swagger API 文档 |

### AI 模型

| 提供商 | 模型 | 用途 |
|--------|------|------|
| 智谱AI | glm-4-flash | 对话（默认） |
| 智谱AI | embedding-3 | 文本嵌入 |
| DeepSeek | deepseek-chat | 对话（openai profile） |
| Anthropic | claude-3-5-sonnet | 对话（anthropic profile） |

## 3. 环境准备

### 前提条件

- Java 17+
- Maven 3.6+
- MySQL 8.x
- 至少一个 AI 模型的 API Key

### 配置环境变量

```bash
# 数据库
export DB_URL=jdbc:mysql://localhost:3306/ai_develop?useSSL=false&serverTimezone=Asia/Shanghai
export DB_USERNAME=root
export DB_PASSWORD=your-password

# AI 模型（至少配一个）
export ZHIPUAI_API_KEY=your-zhipuai-key
# 或
export OPENAI_API_KEY=your-deepseek-key
export OPENAI_BASE_URL=https://api.deepseek.com
# 或
export ANTHROPIC_API_KEY=your-anthropic-key
```

### 创建数据库

```sql
CREATE DATABASE ai_develop DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

JPA 会自动创建表（`spring.jpa.hibernate.ddl-auto=update`）。

## 4. 启动项目

```bash
# 使用智谱AI（默认）
mvn spring-boot:run

# 使用 DeepSeek
mvn spring-boot:run -Dspring-boot.run.profiles=openai

# 使用 Claude
mvn spring-boot:run -Dspring-boot.run.profiles=anthropic
```

## 5. 访问页面

| 页面 | 地址 | 说明 |
|------|------|------|
| 聊天界面 | http://localhost:8080/index.html | 支持 SSE 流式对话 |
| 成本管理 | http://localhost:8080/cost.html | 成本统计看板 |
| Swagger | http://localhost:8080/swagger-ui.html | API 文档和调试 |
| 健康检查 | http://localhost:8080/health | 服务状态 |

## 6. 项目架构

```
┌─────────────────────────────────────┐
│         前端 (HTML / CSS / JS)       │
│    index.html    cost.html          │
├─────────────────────────────────────┤
│       Controller (REST + SSE)        │
│  ChatController  AiCostController   │
├──────────────┬──────────────────────┤
│  ChatService  │    RAG Pipeline      │
│  (对话核心)   │    (检索增强生成)     │
├──────────────┴──────────────────────┤
│  Function Calling │ Cost │ Cache    │
│  (工具调用)       │(成本)│(缓存)     │
├─────────────────────────────────────┤
│    Spring AI (ChatClient/Model)      │
│    VectorStore │ EmbeddingModel      │
├─────────────────────────────────────┤
│    MySQL      │    Caffeine Cache    │
└─────────────────────────────────────┘
```

## 7. 核心目录结构

```
src/main/java/com/example/aidevelop/
├── config/                     # 配置类
│   ├── AiModelConfig.java      #   AI 模型 + ChatClient 配置
│   ├── CacheConfig.java        #   多级缓存配置
│   ├── PromptProperties.java   #   Prompt 模板配置
│   ├── RagProperties.java      #   RAG 参数配置
│   ├── VectorStoreConfig.java  #   向量库 + 文档加载
│   └── SwaggerConfig.java      #   Swagger 配置
├── controller/                 # REST 控制器
│   ├── ChatController.java     #   聊天 + RAG 检索 API
│   ├── AiCostController.java   #   成本统计 API
│   ├── PromptController.java   #   Prompt 管理 API
│   ├── ModelController.java    #   模型信息 API
│   └── HealthController.java   #   健康检查
├── model/
│   ├── dto/chat/               #   聊天请求/响应 DTO
│   ├── dto/rag/                #   RAG 检索结果 DTO
│   └── entity/                 #   JPA 实体（Loan, AiCallLog 等）
├── repository/                 # Spring Data JPA 仓库
├── service/
│   ├── ChatService.java        #   聊天服务接口
│   ├── impl/ChatServiceImpl.java #  聊天核心实现
│   ├── cost/                   #   成本计算和统计
│   ├── function/               #   Function Calling 函数
│   │   ├── LoanQueryFunction.java
│   │   ├── RepaymentQueryFunction.java
│   │   └── RiskAssessmentFunction.java
│   ├── prompt/                 #   Prompt 模板管理
│   ├── cache/                  #   缓存服务
│   └── rag/                    #   RAG 检索服务
│       ├── QueryRewriteService.java
│       ├── QueryExpansionService.java
│       ├── BM25Service.java
│       ├── HybridSearchService.java
│       ├── RerankService.java
│       ├── RagPipelineService.java
│       └── RagEvaluationService.java
├── interceptor/                # AOP 切面（调用日志）
├── scheduled/                  # 定时任务（成本统计）
└── exception/                  # 全局异常处理
```

## 8. 常见启动问题

### 数据库连接失败

```
Communications link failure
```

检查：MySQL 是否启动、DB_URL/DB_USERNAME/DB_PASSWORD 是否正确、数据库是否已创建。

### API Key 未配置

```
API key is required
```

检查：环境变量是否正确设置。智谱AI 需要 `ZHIPUAI_API_KEY`。

### 端口被占用

```
Web server failed to start. Port 8080 was already in use.
```

解决：`kill $(lsof -t -i:8080)` 或修改 `server.port`。

### 向量库初始化失败

```
Error creating bean with name 'vectorStore'
```

检查：embedding 模型 API Key 是否配置。首次启动需要调用 embedding API 构建向量。

### Maven 编译失败

检查 Java 版本是否 17+：`java -version`。清理重新编译：`mvn clean compile`。
