---
title: AI Agent 学习资料全集（Obsidian版）
aliases:
  - AI Agent 学习资料全集
  - AI Agent All In One
tags:
  - ai-agent
  - llm
  - rag
  - function-calling
  - prompt-engineering
  - obsidian
source: /docs
updated: 2026-05-09
---

# AI Agent 学习资料全集（Obsidian版）

> [!info] 说明
> 本文件为 `docs/` 目录学习文档的完整合并版，面向 Obsidian 优化（frontmatter + wikilink 导航）。
> 内容保持原文，不做删减，适合直接备份到个人知识库。

## 快速导航

1. [[#来源文件：README.md|README.md]]
2. [[#来源文件：00-ai-agent-system-blueprint.md|00-ai-agent-system-blueprint.md]]
3. [[#来源文件：01-quick-start.md|01-quick-start.md]]
4. [[#来源文件：02-chat-basics.md|02-chat-basics.md]]
5. [[#来源文件：03-multi-llm.md|03-multi-llm.md]]
6. [[#来源文件：04-prompt-engineering.md|04-prompt-engineering.md]]
7. [[#来源文件：05-function-calling.md|05-function-calling.md]]
8. [[#来源文件：06-rag-basics.md|06-rag-basics.md]]
9. [[#来源文件：07-rag-advanced.md|07-rag-advanced.md]]
10. [[#来源文件：08-cost-and-observability.md|08-cost-and-observability.md]]
11. [[#来源文件：09-ai-form-filling-demo.md|09-ai-form-filling-demo.md]]
12. [[#来源文件：10-agent-loop-design.md|10-agent-loop-design.md]]
13. [[#来源文件：AI_LEARNING_PATH.md|AI_LEARNING_PATH.md]]

---

## 来源文件：README.md

> [!note] 原文链接
> [[README.md]]

# AI Agent 开发学习文档

本项目是 Java 开发者学习 AI Agent 开发的实战教程，基于 Spring Boot 3.3 + Spring AI 构建。每个文档对应一个独立的知识点，按难度递进排列。

## 学习路线

```
第 1 周：基础入门
 ┌───────────────┐   ┌───────────────┐
 │ 01 快速开始    │ → │ 02 基础对话    │
 │ 架构 + 环境    │   │ 流式 + 历史    │
 └───────────────┘   └───────────────┘

第 2 周：核心技能
 ┌───────────────┐   ┌───────────────┐
 │ 03 多模型接入  │ → │ 04 Prompt 工程 │
 │ Profile 切换   │   │ 模板 + 提示词  │
 └───────────────┘   └───────────────┘

第 3 周：Agent 能力
 ┌───────────────┐   ┌───────────────┐
 │ 05 Function   │ → │ 06 RAG 基础    │
 │ Calling       │   │ 向量 + 知识库  │
 └───────────────┘   └───────────────┘

第 4 周：进阶优化
 ┌───────────────┐   ┌───────────────┐
 │ 07 RAG 进阶   │ → │ 08 成本与运维  │
 │ 混合 + 管道   │   │ AOP + 缓存    │
 └───────────────┘   └───────────────┘
```

## 文档索引

| 文档 | 主题 | 难度 | 关键代码文件 |
|------|------|------|------------|
| [00-ai-agent-system-blueprint.md](00-ai-agent-system-blueprint.md) | 系统化蓝图：架构、能力地图与演进路线 | ★ | 全局总览（无代码实现） |
| [01-quick-start.md](01-quick-start.md) | 项目架构总览与快速开始 | ★ | `AiDevelopApplication.java`, `pom.xml`, `application.yml` |
| [02-chat-basics.md](02-chat-basics.md) | 基础对话、流式响应、对话历史 | ★ | `ChatServiceImpl.java`, `ChatController.java`, `Conversation.java` |
| [03-multi-llm.md](03-multi-llm.md) | 多 LLM 接入、Profile 切换 | ★★ | `AiModelConfig.java`, `application-*.yml` |
| [04-prompt-engineering.md](04-prompt-engineering.md) | Prompt 模板管理、系统提示词 | ★★ | `PromptService.java`, `PromptProperties.java` |
| [05-function-calling.md](05-function-calling.md) | AI 函数调用、工具使用 | ★★★ | `LoanQueryFunction.java`, `RiskAssessmentFunction.java` |
| [06-rag-basics.md](06-rag-basics.md) | RAG 基础、向量检索、知识库 | ★★★ | `VectorStoreConfig.java`, `RagProperties.java` |
| [07-rag-advanced.md](07-rag-advanced.md) | 查询重写、混合检索、重排序、管道 | ★★★ | `BM25Service.java`, `HybridSearchService.java`, `RagPipelineService.java` |
| [08-cost-and-observability.md](08-cost-and-observability.md) | 成本管理、AOP 日志、缓存 | ★★ | `AiCallLoggerAspect.java`, `CacheConfig.java`, `AiCostStatisticsService.java` |
| [10-agent-loop-design.md](10-agent-loop-design.md) | 从 Chat+RAG 升级到 Agent Loop 的架构设计 | ★★★★ | `agent/*`（规划新增） |
| [AI_LEARNING_PATH.md](AI_LEARNING_PATH.md) | 4 周学习路线图 | - | 全部 |

---

## 来源文件：00-ai-agent-system-blueprint.md

> [!note] 原文链接
> [[00-ai-agent-system-blueprint.md]]

# AI Agent 学习项目系统化蓝图（架构与技术全景）

## 1. 这份蓝图解决什么问题

你现在看到的 `docs` 文档是按专题拆分的，学习时容易出现两个痛点：
- 知道“每个点怎么做”，但不清楚它们在系统里如何协同
- 学到 RAG、Function Calling、Prompt 后，不确定下一步该怎么形成 Agent 能力

这份文档的目标是给你一个“从上到下”的认知框架：
- 先看系统全貌，再看每个模块的角色
- 明确模块之间的调用关系、数据流和演进路线
- 形成从 Chat 应用进化到 Agent 系统的直观蓝图

---

## 2. 系统定位与核心能力

本项目定位：**Java 开发者学习 AI Agent 开发的工程化训练场**。  
它不是单纯聊天 Demo，而是逐步覆盖 AI 应用核心能力的“能力拼图”。

当前能力版图可以概括为：
1. **对话能力**：阻塞式 + 流式（SSE）对话，多轮历史管理
2. **模型能力**：多 LLM 提供商接入与切换（Profile）
3. **提示词能力**：Prompt 模板化管理与实验
4. **工具能力**：Function Calling 调用后端业务函数
5. **知识能力**：RAG 检索增强（基础检索 + 进阶优化）
6. **运营能力**：成本统计、调用日志、缓存和调度
7. **演进能力**：向 Agent Loop（Plan/Act/Observe/Reflect）升级

---

## 3. 全局架构图（概念层）

可以把系统看成 6 层：

1. **交互层（UI/API）**
   - 前端页面（聊天、成本看板、表单示例）
   - REST API（聊天、检索、评估、成本）

2. **编排层（Application Orchestration）**
   - `ChatController` 等控制器作为入口
   - 将请求路由到聊天、RAG、函数、统计等服务

3. **智能层（AI Runtime）**
   - `ChatClient`/ChatModel 执行 LLM 调用
   - Prompt 模板装配、上下文拼接
   - Function Calling 工具触发

4. **知识层（RAG）**
   - 文档加载与切分
   - Embedding 向量化
   - 向量检索/混合检索/重排序/管道策略

5. **业务与数据层（Domain + Persistence）**
   - 贷款/还款/风险等业务函数
   - 会话、日志、成本统计等实体与仓储

6. **治理层（Observability & Optimization）**
   - AOP 调用日志
   - 成本计算与统计
   - 缓存、定时任务、策略参数管理

这 6 层对应了一个完整 AI 应用的“工程闭环”：  
**可对话 -> 可调用工具 -> 可检索知识 -> 可观测可优化 -> 可向 Agent 演进**。

---

## 4. 核心请求链路（你最该先理解）

### 4.1 普通聊天链路

用户输入 -> Chat API -> 读取/创建会话 -> 拼接历史上下文 -> 调用 LLM -> 返回答案 -> 持久化会话

你学习重点：
- 什么是上下文窗口与滑动窗口
- 为什么要管理历史消息长度
- 为什么 Prompt 装配影响输出质量

### 4.2 流式聊天链路（SSE）

用户输入 -> 流式 API -> LLM 分片输出 -> 服务端逐片推送 -> 前端逐步渲染

你学习重点：
- 流式与阻塞式在体验和成本上的差异
- SSE 在 AI 场景中的实用性（实现简单、足够好）

### 4.3 RAG 链路

用户问题 -> 查询预处理（重写/扩展）-> 检索（向量/混合）-> 精排（可选）-> 拼接上下文 -> 生成回答

你学习重点：
- “召回率”与“精确率”的平衡
- 为什么需要 query rewrite/expansion/rerank
- 为什么需要评估指标（Recall、MRR、NDCG）

### 4.4 Function Calling 链路

用户意图 -> LLM 判断是否需要工具 -> 触发业务函数 -> 函数返回结构化结果 -> LLM 组织自然语言答案

你学习重点：
- 工具描述（schema）如何影响调用成功率
- 函数结果为什么必须结构化

---

## 5. 模块地图（按学习价值排序）

###[A] 对话内核（Chat Basics）
- 关键词：`ChatClient`、消息角色、上下文管理、SSE
- 学习价值：构建“AI 能说话”的基础
- 典型误区：只会调 API，不会做会话管理

###[B] 多模型接入（Multi-LLM）
- 关键词：Provider 抽象、Profile 切换、模型对比
- 学习价值：把模型当“可替换基础设施”
- 典型误区：模型强绑定在业务代码中，难以切换

###[C] Prompt 工程
- 关键词：系统提示词、模板化、A/B 对比
- 学习价值：最便宜的质量提升手段
- 典型误区：把 Prompt 写死在业务代码里

###[D] Function Calling（工具调用）
- 关键词：工具定义、参数结构、调用约束
- 学习价值：从“聊天”进入“执行任务”
- 典型误区：函数返回不可解析文本，导致不稳定

###[E] RAG 基础与进阶
- 关键词：Embedding、向量检索、混合检索、重排序、管道策略
- 学习价值：让回答基于私有知识，降低幻觉
- 典型误区：只做向量检索，不做评估闭环

###[F] 成本与可观测性
- 关键词：AOP 日志、Token/费用统计、缓存策略
- 学习价值：把 demo 变成“可运营系统”
- 典型误区：没有观测数据，无法优化

###[G] Agent Loop 设计
- 关键词：Plan/Act/Observe/Reflect、工具路由、轨迹追踪
- 学习价值：迈向“自主执行系统”
- 典型误区：直接追求复杂多 Agent，忽略单 Agent MVP

---

## 6. 关键技术点一览（建立知识坐标）

1. **Spring AI 抽象层**
   - 统一多模型调用方式
   - 降低 provider 锁定风险

2. **Prompt 外部化**
   - 将行为策略从代码剥离
   - 支持迭代和版本管理

3. **RAG 管道化**
   - 不把检索当单步操作
   - 通过策略组合提升质量

4. **工具化能力**
   - 让模型可调用“真实系统能力”
   - 从问答转向任务执行

5. **可观测性与成本治理**
   - 每次调用可追踪、可度量
   - 支撑优化和生产化决策

6. **缓存与调度**
   - 降低延迟与成本
   - 对稳定性和用户体验都有帮助

---

## 7. 从“功能清单”到“系统思维”的演进路径

### 阶段 1：能跑（Chat）
- 完成基础对话、流式输出、会话管理
- 目标：让系统具备稳定对话能力

### 阶段 2：能控（Prompt + 多模型）
- 支持模型切换与 Prompt 管理
- 目标：可控地调优回答质量与风格

### 阶段 3：能查（RAG）
- 打通知识库检索与生成
- 目标：回答“有依据”

### 阶段 4：能做（Function Calling）
- 让模型调用工具执行任务
- 目标：从“回答问题”升级为“完成动作”

### 阶段 5：能优（可观测 + 成本）
- 建立指标、日志和优化机制
- 目标：具备工程可持续性

### 阶段 6：能自主（Agent Loop）
- 建立执行闭环与轨迹追踪
- 目标：形成可扩展 Agent 架构

---

## 8. 你现在最该抓住的 3 个主线

1. **主线一：上下文管理能力**
   - 对话历史、Prompt 组织、知识注入本质上都在做上下文管理

2. **主线二：检索与工具双引擎**
   - RAG 解决“知道什么”，Function 解决“能做什么”

3. **主线三：可观测驱动迭代**
   - 没有指标就没有优化，AI 应用必须数据化演进

---

## 9. 对应文档导航（按蓝图阅读顺序）

建议按这个顺序看：

1. `01-quick-start.md`：系统入口与目录地图  
2. `02-chat-basics.md`：对话内核  
3. `03-multi-llm.md`：模型抽象  
4. `04-prompt-engineering.md`：提示词体系  
5. `05-function-calling.md`：工具调用  
6. `06-rag-basics.md`：RAG 最小闭环  
7. `07-rag-advanced.md`：检索优化策略  
8. `08-cost-and-observability.md`：可观测与成本治理  
9. `10-agent-loop-design.md`：面向下一阶段的 Agent 架构  
10. `AI_LEARNING_PATH.md`：按周执行节奏

---

## 10. 一句话总结（你的直观蓝图）

这个项目本质上是在构建一个渐进式 AI 系统：  
**以 Chat 为入口，以 RAG 与 Function 为能力核心，以观测与成本为治理闭环，最终演进为 Agent Loop 的自主执行架构。**

---

## 来源文件：01-quick-start.md

> [!note] 原文链接
> [[01-quick-start.md]]

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
| OpenAI Compatible (GLM) | glm-4.5-flash | 对话（openai profile） |

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
export OPENAI_API_KEY=your-openai-compatible-key
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

# 使用 OpenAI Compatible（GLM）
mvn spring-boot:run -Dspring-boot.run.profiles=openai
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

---

## 来源文件：02-chat-basics.md

> [!note] 原文链接
> [[02-chat-basics.md]]

# 02 - 基础对话：ChatClient、流式响应与对话管理

本节讲解如何使用 Spring AI 构建完整的 AI 对话功能，包括阻塞式调用、流式响应、对话历史管理和前端交互。

技术栈：Spring Boot 3.3.5 + Spring AI 1.0.0-M5 + Spring MVC（SSE 流式输出）

---

## 1. 核心概念

### ChatModel vs ChatClient

Spring AI 提供两个层次的 API：

| | ChatModel | ChatClient |
|---|---|---|
| 层次 | 底层接口 | 高层封装 |
| 用途 | 直接调用 LLM API | 通过 Builder 模式组合 Advisor |
| 功能 | 发送消息、获取响应 | 自动注入系统提示、对话记忆、RAG、Function Calling |
| 类比 | JDBC | JPA / MyBatis |

本项目使用 `ChatClient` 作为统一入口，在 `AiModelConfig` 中完成所有配置。

### 消息类型

```java
// MessageRole.java - 三种消息角色
public enum MessageRole {
    SYSTEM,    // 系统提示词：定义 AI 的行为和角色
    USER,      // 用户消息：用户的输入
    ASSISTANT  // AI 响应：模型的输出
}
```

每条消息由 `Message` 实体承载，包含 id、role、content、timestamp、model 五个字段。

### Token 与上下文窗口

- **Token** 是 LLM 计费和处理的最小单位，大约 1 个汉字 = 1-2 个 Token
- **上下文窗口** 是模型单次能处理的最大 Token 数（如 deepseek-chat 支持 64K）
- 对话历史越长，消耗的 Token 越多，因此需要滑动窗口控制

### SSE（Server-Sent Events）

SSE 是一种基于 HTTP 的单向实时推送协议。服务端保持连接打开，持续发送数据块给客户端。与 WebSocket 相比，SSE 更简单，天然支持 HTTP/2，适合 LLM 流式输出场景。

---

## 2. Spring AI ChatClient 详解

`AiModelConfig.java` 是项目的核心配置类，通过 `@Profile` 注解为不同 LLM 提供商创建对应的 `ChatClient` Bean。

### Builder 模式

```java
@Bean
@Profile("openai")
public ChatClient chatClientForOpenAI(
        @Qualifier("openAiChatModel") ChatModel chatModel,
        @Qualifier("vectorStore") VectorStore vectorStore,
        ChatMemory chatMemory) {

    // RAG 检索参数（从配置文件读取）
    SearchRequest searchRequest = SearchRequest.defaults()
            .topK(ragProperties.getTopK())           // 返回 Top 5 文档
            .similarityThreshold(ragProperties.getSimilarityThreshold())  // 相似度阈值 0.2
            .build();

    return ChatClient.builder(chatModel)
            .defaultSystem(promptService.getSystemPrompt())  // 从文件加载系统提示词
            .defaultAdvisors(
                new MessageChatMemoryAdvisor(chatMemory),        // 对话记忆
                new QuestionAnswerAdvisor(vectorStore, searchRequest)  // RAG 检索
            )
            .defaultFunctions(                                  // Function Calling
                "loanQueryFunction",
                "repaymentQueryFunction",
                "riskAssessmentFunction"
            )
            .build();
}
```

### Advisor 职责

| Advisor | 作用 | 工作原理 |
|---|---|---|
| `MessageChatMemoryAdvisor` | 多轮对话记忆 | 自动从 ChatMemory 读取历史消息并注入到 Prompt |
| `QuestionAnswerAdvisor` | RAG 检索增强 | 将用户问题与向量库匹配，把相关文档片段注入到上下文 |

`defaultFunctions` 注册了三个 Function Calling 函数，LLM 会在需要时自动调用它们查询数据库。系统提示词从 `src/main/resources/prompts/system/default.txt` 文件加载，通过 `PromptService` 管理。

---

## 3. 阻塞式对话实现

`ChatServiceImpl.chat()` 方法实现了完整的阻塞式对话流程。

### 调用流程

```
用户请求 -> 获取/创建对话 -> 保存用户消息 -> 构建含历史的 Prompt
    -> 调用 ChatClient -> 获取响应 -> 保存 AI 消息 -> 返回 ChatResponse
```

### 关键代码

```java
// ChatServiceImpl.java - chat() 方法核心逻辑

// 1. 获取或创建对话（支持多轮对话）
Conversation conversation = getOrCreateConversation(request.getConversationId());

// 2. 保存用户消息到历史
Message userMessage = new Message(UUID.randomUUID().toString(),
    MessageRole.USER, request.getMessage(), LocalDateTime.now(), null);
conversation.addMessage(userMessage);

// 3. 构建包含历史消息的提示词
String prompt = buildPromptWithHistory(conversation);

// 4. 调用 AI 模型（阻塞式）
org.springframework.ai.chat.model.ChatResponse aiResponse = chatClient.prompt()
    .user(prompt)
    .call()            // 阻塞调用
    .chatResponse();   // 获取完整响应（含元数据）

String responseContent = aiResponse.getResult().getOutput().getContent();
```

### ChatResponse 结构

```java
// ChatResponse.java - 返回给前端的响应 DTO
public class ChatResponse {
    private String conversationId;  // 对话 ID，前端下次请求时带回
    private String message;         // AI 的回复内容
    private String model;           // 使用的模型名称（如 deepseek-chat）
    private Integer tokensUsed;     // 本次消耗的 Token 数
    private Long responseTime;      // 响应耗时（毫秒）
}
```

`tokensUsed` 从 `aiResponse.getMetadata().getUsage().getTotalTokens()` 获取，`model` 从 `aiResponse.getMetadata().getModel()` 获取。

---

## 4. 流式响应实现

`ChatServiceImpl.streamChat()` 方法返回 `Flux<String>`（响应式流），实现逐字输出。

### 关键代码

```java
// ChatServiceImpl.java - streamChat() 方法核心逻辑

StringBuilder fullResponse = new StringBuilder();

return chatClient.prompt()
    .user(prompt)
    .stream()          // 流式调用（非阻塞）
    .content()         // 获取内容流
    .doOnNext(chunk -> {
        fullResponse.append(chunk);     // 逐块拼接完整响应
    })
    .doOnComplete(() -> {
        // 流结束后保存完整响应到对话历史
        Message assistantMessage = new Message(..., fullResponse.toString(), ...);
        conversation.addMessage(assistantMessage);
        conversationRepository.save(conversation);
    })
    .doOnError(error -> {
        log.error("流式响应出错", error);
    });
```

### Controller 端的 SSE 映射

```java
// ChatController.java
@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> streamChat(@Valid @RequestBody ChatRequest request) {
    return chatService.streamChat(request);
}
```

`MediaType.TEXT_EVENT_STREAM_VALUE` 告诉浏览器这是 SSE 响应。Spring WebFlux 自动将 `Flux<String>` 转换为 SSE 格式（`data: chunk\n\n`）。

### 流式 vs 阻塞式对比

| | 阻塞式 (`/api/chat`) | 流式 (`/api/chat/stream`) |
|---|---|---|
| 返回类型 | `ChatResponse`（完整 JSON） | `Flux<String>`（SSE 流） |
| 用户体验 | 等待全部生成后才显示 | 逐字显示，体验更自然 |
| Token 统计 | 从响应元数据中获取完整统计 | 流式模式下不包含元数据 |
| 历史保存 | 调用后立即保存 | `doOnComplete` 中保存 |
| 适用场景 | API 调用、需要元数据 | 聊天界面、实时交互 |

---

## 5. 对话历史管理

### Conversation 实体

```java
// Conversation.java
public class Conversation {
    private String conversationId;
    private List<Message> messages = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int maxHistorySize = 10;  // 最大历史条数

    public void addMessage(Message message) {
        messages.add(message);
        // 滑动窗口：移除最早的非 SYSTEM 消息
        while (messages.stream()
                .filter(m -> m.getRole() != MessageRole.SYSTEM)
                .count() > maxHistorySize) {
            messages.stream()
                .filter(m -> m.getRole() != MessageRole.SYSTEM)
                .findFirst()
                .ifPresent(messages::remove);
        }
        this.updatedAt = LocalDateTime.now();
    }
}
```

### 滑动窗口机制

核心规则：始终保留 SYSTEM 消息，只对 USER/ASSISTANT 消息进行淘汰。

```
初始状态: [SYSTEM, USER1, ASSISTANT1, ..., USER10, ASSISTANT10]  (10 轮)
添加 USER11: [SYSTEM, USER2, ASSISTANT2, ..., USER10, ASSISTANT10, USER11]  (淘汰 USER1)
```

### ConversationRepository - 内存存储

```java
// ConversationRepository.java
@Repository
public class ConversationRepository {
    private final Map<String, Conversation> storage = new ConcurrentHashMap<>();

    public Conversation save(Conversation conversation) { ... }
    public Optional<Conversation> findById(String conversationId) { ... }
    public void delete(String conversationId) { ... }
    public void deleteAll() { ... }
}
```

使用 `ConcurrentHashMap` 保证线程安全。这是简化实现，适合开发和学习。生产环境应替换为 Redis 或数据库持久化。

### buildPromptWithHistory - 历史拼接

```java
// 将对话历史拼接为文本格式
private String buildPromptWithHistory(Conversation conversation) {
    return conversation.getMessages().stream()
        .filter(m -> m.getRole() != MessageRole.SYSTEM)
        .map(m -> {
            String roleLabel = m.getRole() == MessageRole.USER ? "用户" : "助手";
            return roleLabel + ": " + m.getContent();
        })
        .collect(Collectors.joining("\n")) + "\n助手: ";
}
```

将历史消息格式化为"用户/助手"交替的文本，附加到 Prompt 中，使 LLM 能理解多轮对话上下文。

---

## 6. 前端聊天界面

前端由三个文件组成：

| 文件 | 职责 |
|---|---|
| `index.html` | 页面结构和布局 |
| `js/chat.js` | 交互逻辑（`ChatApp` 类） |
| `css/chat.css` | 样式 |

### ChatApp 类核心逻辑

`ChatApp` 通过下拉框 `modeSelect` 切换两种模式：

**普通模式**（`sendNormalMessage`）：使用 `fetch` 发送 POST 请求到 `/api/chat`，等待完整 JSON 响应后一次性渲染。

**流式模式**（`sendStreamMessage`）：使用 `fetch` + `ReadableStream` 读取 SSE 流。逐块解析 `data:` 前缀的内容，通过 `marked.js` 实时渲染 Markdown：

```javascript
// chat.js - 流式读取核心
const reader = response.body.getReader();
const decoder = new TextDecoder('utf-8');

while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    const chunk = decoder.decode(value, { stream: true });
    // 解析 SSE 格式：data: content
    // 每收到一块就调用 renderMarkdown() 更新界面
    this.renderMarkdown(contentDiv, fullText);
}
```

`marked.js` 负责 Markdown 渲染，支持 GFM 语法（表格、列表、代码块等）。

---

## 7. 动手实验

### 实验 1：修改系统提示词，观察 AI 行为变化

修改 `src/main/resources/prompts/system/default.txt` 文件内容。例如：

- 将角色从"金融系统AI助手"改为"技术面试官"
- 移除 Function Calling 相关的提示
- 添加新的回答格式要求

重启应用后发送相同问题，对比 AI 行为差异。系统提示词是控制 AI 行为最直接的手段。

### 实验 2：调整滑动窗口大小，观察多轮对话效果

修改 `Conversation.java` 中 `maxHistorySize` 的默认值：

```java
private int maxHistorySize = 3;  // 从 10 改为 3
```

进行 5 轮以上对话，观察 AI 是否还能理解早期对话内容。窗口越小，AI 的"记忆"越短。

### 实验 3：添加对话 Token 统计接口

在 `ChatController` 中添加新接口，返回指定对话的累计 Token 消耗：

```java
@GetMapping("/{conversationId}/stats")
public Map<String, Object> getConversationStats(@PathVariable String conversationId) {
    Conversation conv = conversationRepository.findById(conversationId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    // 统计消息数、模型信息等
    // 返回 Map 或创建专用 DTO
}
```

提示：阻塞式调用时 `Message.model` 有值（从元数据获取），流式调用时为 null。可考虑在流式模式的 `doOnComplete` 中补充模型信息。

---

## 8. 关键代码文件

| 文件 | 关注点 |
|---|---|
| `src/main/java/.../service/impl/ChatServiceImpl.java` | chat() 和 streamChat() 的完整实现、历史拼接逻辑 |
| `src/main/java/.../controller/ChatController.java` | REST 接口定义、SSE 映射、Swagger 注解 |
| `src/main/java/.../model/entity/Conversation.java` | 滑动窗口的 addMessage() 方法 |
| `src/main/java/.../model/entity/Message.java` | 消息结构（id, role, content, timestamp, model） |
| `src/main/java/.../model/dto/chat/ChatRequest.java` | 请求 DTO（message, conversationId, model, temperature） |
| `src/main/java/.../model/dto/chat/ChatResponse.java` | 响应 DTO（conversationId, message, model, tokensUsed, responseTime） |
| `src/main/java/.../repository/ConversationRepository.java` | 基于 ConcurrentHashMap 的内存存储 |
| `src/main/java/.../config/AiModelConfig.java` | ChatClient Bean 的构建和 Advisor 配置 |
| `src/main/resources/prompts/system/default.txt` | 系统提示词（外部文件管理） |
| `src/main/resources/static/index.html` | 聊天界面 HTML |
| `src/main/resources/static/js/chat.js` | ChatApp 类、流式读取、Markdown 渲染 |
| `src/main/resources/static/css/chat.css` | 界面样式 |

路径中的 `...` 代表 `com/example/aidevelop`。

---

## 来源文件：03-multi-llm.md

> [!note] 原文链接
> [[03-multi-llm.md]]

# 03 - 多 LLM 接入：Provider 抽象与 Profile 切换

本节讲解如何使用 Spring AI 的 Provider 抽象机制接入多个 LLM 提供商，并通过 Spring Profile 实现运行时切换。

技术栈：Spring Boot 3.3.5 + Spring AI 1.0.0-M5

---

## 1. 核心概念

### 为什么要支持多个 LLM

| 原因 | 说明 |
|---|---|
| 成本优化 | 不同模型定价差异大，简单问题用便宜模型，复杂推理用高端模型 |
| 模型能力 | 不同模型在不同任务上表现不同（代码生成、中文理解、推理能力） |
| 可用性 | 某个提供商服务中断时可以快速切换 |
| 合规要求 | 部分场景需要使用国产模型或私有化部署模型 |

### Spring AI 的 Provider 抽象

Spring AI 的核心设计：所有 LLM 提供商都实现相同的 `ChatModel` 接口。无论底层是 OpenAI 兼容模型还是智谱 AI，上层代码通过 `ChatClient` 调用时完全一致。

```
ChatController -> ChatService -> ChatClient -> ChatModel（接口）
                                                  |
                                    +-------------+-------------+
                                    |             |             |
                              OpenAiChatModel                 ZhipuAiChatModel
```

切换 LLM 只需要更换 `ChatModel` 的实现 Bean，业务代码无需任何修改。

### Spring Profile 切换机制

Spring Profile 是一种条件化配置机制。在 `AiModelConfig` 中，每个 `@Profile("xxx")` 注解的 Bean 方法只在该 Profile 激活时才会创建。通过启动参数 `spring.profiles.active` 指定使用哪个 Profile。

---

## 2. 支持的 LLM 提供商

本项目当前接入三个提供商：

| 提供商 | 配置文件 | Profile | Chat Model | Embedding Model | 用途 |
|---|---|---|---|---|---|
| DeepSeek（通过 OpenAI 兼容接口） | `application-openai.yml` | `openai` | `deepseek-chat` | - | 对话（默认） |
| OpenAI Compatible（GLM） | `application-openai.yml` | `openai` | `glm-4.5-flash` | - | 对话 |
| 智谱 AI | `application.yml`（zhipuai 段） | default（始终加载） | `glm-4-flash` | `embedding-3` | RAG 向量化 |

注意：智谱 AI 始终加载，因为它的 Embedding 模型用于 RAG 向量化，与对话模型解耦。向量存储 `VectorStoreConfig` 通过 `@Qualifier("zhiPuAiEmbeddingModel")` 显式指定使用智谱 AI 的 Embedding。

---

## 3. 配置文件详解

### application-openai.yml（DeepSeek）

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}               # 从环境变量读取 API Key
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}  # DeepSeek 使用兼容接口，需替换 base-url
      chat:
        options:
          model: deepseek-chat                  # DeepSeek 对话模型名称
          temperature: 0.7                      # 温度参数：越高越随机，0.7 是平衡值
          max-tokens: 1000                      # 单次响应最大 Token 数
```

DeepSeek 提供 OpenAI 兼容接口，所以使用 Spring AI 的 `spring-ai-openai-spring-boot-starter`，只需将 `base-url` 指向 DeepSeek 的 API 地址（通过环境变量 `OPENAI_BASE_URL` 配置）。

### application-openai.yml（GLM）

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}                # 从环境变量读取 OpenAI 兼容 API Key
      base-url: ${OPENAI_BASE_URL}              # OpenAI 兼容接口地址（可配置为 GLM）
      chat:
        options:
          model: ${OPENAI_CHAT_MODEL:glm-4.5-flash}  # 默认 GLM 模型
          temperature: 0.7
          max-tokens: 1000
```

项目当前使用 OpenAI 兼容 starter，配置前缀为 `spring.ai.openai`。

### application.yml 中的智谱 AI 配置

```yaml
spring:
  ai:
    zhipuai:
      api-key: ${ZHIPUAI_API_KEY:}
      embedding:
        enabled: true              # 只启用 Embedding 功能
```

智谱 AI 在本项目中不用于对话，只用于 Embedding（将文档向量化供 RAG 检索使用）。`spring-ai-zhipuai-spring-boot-starter` 会自动创建 `zhiPuAiEmbeddingModel` Bean。

### application.yml 中的 Profile 默认值

```yaml
spring:
  profiles:
    active: openai    # 默认激活 openai Profile（即 DeepSeek）
```

启动时不指定 Profile 则使用此默认值。

---

## 4. AiModelConfig 详解

`AiModelConfig.java` 是多 LLM 切换的核心配置类。

### 类结构

```java
@Slf4j
@Configuration
public class AiModelConfig {

    private final RagProperties ragProperties;   // RAG 参数（从 yml 读取）
    private final PromptService promptService;    // 提示词管理服务

    // 构造器注入
    public AiModelConfig(RagProperties ragProperties, PromptService promptService) { ... }

    @Bean
    public ChatMemory chatMemory() { ... }       // 全局共享的对话记忆

    @Bean @Profile("openai")
    public ChatClient chatClientForOpenAI(...) { ... }

    @Bean @Profile("openai")
    public ChatClient chatClientForOpenAI(...) { ... }
}
```

### 共享的 ChatMemory Bean

```java
@Bean
public ChatMemory chatMemory() {
    return new InMemoryChatMemory();  // 内存实现的对话记忆
}
```

`ChatMemory` 不绑定 Profile，所有 Profile 的 `ChatClient` 共享同一个实例。`InMemoryChatMemory` 是 Spring AI 提供的内存实现，重启后丢失。`MessageChatMemoryAdvisor` 会自动从这里存取历史消息。

### OpenAI Profile 的 ChatClient

```java
@Bean
@Profile("openai")
public ChatClient chatClientForOpenAI(
        @Qualifier("openAiChatModel") ChatModel chatModel,     // Spring AI 自动创建的 OpenAI ChatModel
        @Qualifier("vectorStore") VectorStore vectorStore,
        ChatMemory chatMemory) {

    log.info("初始化 ChatClient，使用提供商: OpenAI (DeepSeek)，启用对话记忆功能");

    SearchRequest searchRequest = SearchRequest.defaults()
            .topK(ragProperties.getTopK())
            .similarityThreshold(ragProperties.getSimilarityThreshold())
            .build();

    return ChatClient.builder(chatModel)
            .defaultSystem(promptService.getSystemPrompt())    // 外部文件管理提示词
            .defaultAdvisors(
                new MessageChatMemoryAdvisor(chatMemory),
                new QuestionAnswerAdvisor(vectorStore, searchRequest)
            )
            .defaultFunctions("loanQueryFunction", "repaymentQueryFunction", "riskAssessmentFunction")
            .build();
}
```

`@Qualifier("openAiChatModel")` 显式指定使用 OpenAI 的 ChatModel Bean，避免多个 ChatModel 实现时冲突。

### OpenAI Profile 的 ChatClient

```java
@Bean
@Profile("openai")
public ChatClient chatClientForOpenAI(
        @Qualifier("openAiChatModel") ChatModel chatModel,  // OpenAI 兼容的 ChatModel
        @Qualifier("vectorStore") VectorStore vectorStore,
        ChatMemory chatMemory) {

    log.info("初始化 ChatClient，使用提供商: OpenAI Compatible(GLM)，启用对话记忆功能");

    // 注意：此处硬编码了 RAG 参数和系统提示词
    SearchRequest searchRequest = SearchRequest.defaults()
            .topK(5).similarityThreshold(0.3).build();

    return ChatClient.builder(chatModel)
            .defaultSystem(""" ... """)    // 内联的系统提示词
            .defaultAdvisors(...)
            .defaultFunctions(...)
            .build();
}
```

当前 OpenAI Profile 统一通过 `promptService.getSystemPrompt()` 从外部文件加载系统提示词。

### Bean 命名与冲突处理

当多个 LLM starter 同时引入时，Spring 容器中会存在多个 `ChatModel` Bean。Spring AI 使用 `@Qualifier` 注解区分：

- `openAiChatModel` - 由 `spring-ai-openai-spring-boot-starter` 自动创建
- `openAiChatModel` - 由 OpenAI 兼容 starter 自动创建

只有与当前 Profile 匹配的 `ChatClient` Bean 会被创建，因此不会冲突。

---

## 5. 如何切换模型

### 通过 Maven 命令切换

```bash
# 使用 DeepSeek（默认，等价于不加 -D 参数）
mvn spring-boot:run -Dspring-boot.run.profiles=openai

# 使用 OpenAI Compatible（GLM）
mvn spring-boot:run -Dspring-boot.run.profiles=openai

# 使用默认配置（application.yml 中的 spring.profiles.active=openai）
mvn spring-boot:run
```

### 通过环境变量切换

```bash
# 设置环境变量
export SPRING_PROFILES_ACTIVE=openai
mvn spring-boot:run
```

### 通过 application.yml 切换

修改 `application.yml` 中的 `spring.profiles.active` 值：

```yaml
spring:
  profiles:
    active: openai
```

### 验证当前使用的模型

启动应用后，日志中会打印：

```
初始化 ChatClient，使用提供商: OpenAI (DeepSeek)，启用对话记忆功能
```

或通过阻塞式聊天接口查看响应中的 `model` 字段。

### 环境变量配置

每个提供商需要配置对应的 API Key：

```bash
# DeepSeek（通过 OpenAI 兼容接口）
export OPENAI_API_KEY="sk-xxx"
export OPENAI_BASE_URL="https://api.deepseek.com"

# OpenAI Compatible（GLM）
export ANTHROPIC_API_KEY="sk-ant-xxx"

# 智谱 AI（Embedding 用）
export ZHIPUAI_API_KEY="xxx"
```

---

## 6. 接入新 LLM 的步骤

以接入阿里通义千问为例。

### 步骤 1：添加 Maven 依赖

在 `pom.xml` 中添加 Spring AI 的通义千问 starter：

```xml
<!-- 注意：确认 spring-ai-bom 中是否已包含此依赖，版本号由 BOM 管理 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-tongyi-spring-boot-starter</artifactId>
</dependency>
```

如果 Spring AI 官方尚未提供该 starter，可查找社区实现或使用 OpenAI 兼容接口（通义千问支持 OpenAI 兼容协议）。

### 步骤 2：创建配置文件

创建 `src/main/resources/application-tongyi.yml`：

```yaml
spring:
  ai:
    tongyi:
      api-key: ${TONGYI_API_KEY}
      chat:
        options:
          model: qwen-plus        # 通义千问模型名称
          temperature: 0.7
          max-tokens: 1000
```

如果使用 OpenAI 兼容接口，则复用 `application-openai.yml` 格式，只修改 `base-url` 和 `model`。

### 步骤 3：添加 AiModelConfig 方法

在 `AiModelConfig.java` 中添加：

```java
@Bean
@Profile("tongyi")
public ChatClient chatClientForTongyi(
        @Qualifier("tongyiChatModel") ChatModel chatModel,
        @Qualifier("vectorStore") VectorStore vectorStore,
        ChatMemory chatMemory) {

    log.info("初始化 ChatClient，使用提供商: 通义千问");

    SearchRequest searchRequest = SearchRequest.defaults()
            .topK(ragProperties.getTopK())
            .similarityThreshold(ragProperties.getSimilarityThreshold())
            .build();

    return ChatClient.builder(chatModel)
            .defaultSystem(promptService.getSystemPrompt())
            .defaultAdvisors(
                new MessageChatMemoryAdvisor(chatMemory),
                new QuestionAnswerAdvisor(vectorStore, searchRequest)
            )
            .defaultFunctions("loanQueryFunction", "repaymentQueryFunction", "riskAssessmentFunction")
            .build();
}
```

### 步骤 4：测试

```bash
# 设置环境变量
export TONGYI_API_KEY="sk-xxx"

# 启动应用
mvn spring-boot:run -Dspring-boot.run.profiles=tongyi
```

发送测试消息，验证对话是否正常。

---

## 7. 动手实验

### 实验 1：比较不同模型对同一问题的回答质量

使用 `openai` Profile 启动应用，发送以下测试问题并验证回答效果：

1. "逾期超过90天的客户应该如何处理？"（规则类问题，测试 RAG 能力）
2. "请计算客户 CUST001 的总欠款金额"（数据查询，测试 Function Calling）
3. "如果客户逾期45天，之前已经有3次逾期记录，应该采取什么措施？"（复杂推理）

对比维度：回答准确性、格式规范性、是否正确引用知识库规则、Function Calling 是否正确触发。

### 实验 2：接入一个新的 LLM 提供商

按照第 6 节的步骤，尝试接入一个新的 LLM（如百度文心一言、Moonshot 等）。重点关注：

- 该提供商是否有 Spring AI 官方 starter
- 如果没有，是否支持 OpenAI 兼容接口
- `@Qualifier` 注解中 ChatModel Bean 的名称

### 实验 3：为不同场景选择最优模型

思考以下场景应该使用哪个模型，并实际测试验证：

| 场景 | 推荐模型 | 理由 |
|---|---|---|
| 简单问答（FAQ） | glm-4-flash / deepseek-chat | 成本低，响应快 |
| 复杂推理（风险评估） | glm-4.5-flash | 推理能力强 |
| 代码生成 | ? | 实际测试后填写 |
| 大批量 Embedding | 智谱 embedding-3 | 国产模型，中文向量化效果好 |

---

## 8. 关键代码文件

| 文件 | 关注点 |
|---|---|
| `src/main/java/.../config/AiModelConfig.java` | @Profile 条件化 Bean、ChatClient 构建、Advisor 配置 |
| `src/main/java/.../config/VectorStoreConfig.java` | 向量库初始化、@Qualifier("zhiPuAiEmbeddingModel") 指定 Embedding 模型 |
| `src/main/java/.../config/RagProperties.java` | RAG 参数配置类（similarityThreshold, topK） |
| `src/main/java/.../config/PromptProperties.java` | 提示词文件路径配置 |
| `src/main/java/.../service/prompt/PromptService.java` | 提示词加载服务（从文件读取系统提示词） |
| `src/main/resources/application.yml` | 公共配置 + 智谱 AI Embedding + 默认 Profile |
| `src/main/resources/application-openai.yml` | DeepSeek 配置（OpenAI 兼容接口） |
| `src/main/resources/application-openai.yml` | OpenAI 兼容（GLM）配置 |
| `src/main/resources/prompts/system/default.txt` | 系统提示词文件 |
| `pom.xml` | spring-ai-bom 版本管理、各 LLM starter 依赖 |

路径中的 `...` 代表 `com/example/aidevelop`。

---

## 来源文件：04-prompt-engineering.md

> [!note] 原文链接
> [[04-prompt-engineering.md]]

# 04 - Prompt 工程：模板管理与系统提示词设计

## 1. 核心概念

### System Prompt vs User Prompt

- **System Prompt**：定义 AI 的角色、行为规则和约束，在每次对话中保持不变
- **User Prompt**：用户实际输入的问题或指令

系统提示词决定了 AI 回答的质量和方向，是 Prompt Engineering 中最重要的部分。

### Prompt 模板的作用

- **外部化**：提示词从代码中分离，修改不需要重新编译
- **版本化**：可以追踪提示词的变更历史
- **热加载**：运行时修改提示词，无需重启应用

### 常用 Prompt Engineering 技巧

| 技巧 | 说明 | 适用场景 |
|------|------|---------|
| Role-playing | 给 AI 设定专业角色 | 专业领域问答 |
| Few-shot | 提供几个示例 | 格式化输出、特定风格 |
| CoT (Chain of Thought) | 引导逐步推理 | 复杂逻辑问题 |
| 约束条件 | 限定回答范围和格式 | 避免跑题、控制输出 |

## 2. 本项目的 Prompt 架构

```
application.yml (app.prompt.*)
       │
       ▼
PromptProperties (@ConfigurationProperties)
       │
       ▼
PromptService (加载 classpath:prompts/ 下的模板文件)
       │
       ▼
AiModelConfig (将 prompt 注入 ChatClient.builder())
```

### PromptProperties

将 `application.yml` 中的 `app.prompt.*` 配置绑定到 Java 对象：

```yaml
app:
  prompt:
    system-prompt-path: classpath:prompts/system-prompt.txt
    rag-qa-prompt-path: classpath:prompts/rag-qa-prompt.txt
    function-calling-prompt-path: classpath:prompts/function-calling-prompt.txt
```

### PromptService

核心职责：
1. 从 `classpath:prompts/` 加载模板文件
2. 支持运行时热加载（`/api/prompt/reload`）
3. 文件不存在时回退到硬编码默认值

```java
@Service
public class PromptService {
    private String systemPrompt;

    @PostConstruct
    public void loadPrompts() {
        // 从文件加载，失败则用默认值
        this.systemPrompt = loadFromFileOrDefault(
            properties.getSystemPromptPath(),
            "你是一个专业的金融助贷系统客服..."
        );
    }

    public void reload() {
        loadPrompts(); // 重新加载
    }
}
```

## 3. 系统提示词设计实战

### 设计原则

1. **明确角色定位**：告诉 AI "你是谁"
2. **界定能力边界**：说明 AI 能做什么、不能做什么
3. **提供决策框架**：帮助 AI 判断不同场景的应对策略
4. **设定输出格式**：规范回答的结构和风格

### 本项目的系统提示词分析

项目根据用户问题类型设计了 4 种决策策略：

```
场景 1: 纯规则类问题（如"逾期怎么处理"）
  -> 直接从知识库检索回答，不调用函数

场景 2: 纯数据类问题（如"查一下贷款 LN001"）
  -> 调用函数查询数据库，用真实数据回答

场景 3: 混合类问题（如"这笔贷款逾期了怎么办"）
  -> 先查数据，再结合规则知识回答

场景 4: 假设/通用问题（如"贷款利率一般是多少"）
  -> 给出一般性建议，提示以实际为准
```

### 不同模型的 Prompt 差异

- **DeepSeek/智谱AI**：提示词较短，模型能较好理解隐含意图
- **GLM（OpenAI 兼容）**：提示词建议保持结构化，包含清晰的决策指令和输出格式要求

这反映了不同模型对指令粒度的需求不同，实际项目中需要针对使用的模型调整。

## 4. Prompt 模板文件

```
src/main/resources/prompts/
├── system-prompt.txt          # 通用系统提示词
├── rag-qa-prompt.txt          # RAG 问答提示词（指导如何使用检索结果）
└── function-calling-prompt.txt # 函数调用引导提示词
```

## 5. Prompt 管理 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/prompt/current` | 查看当前系统提示词 |
| POST | `/api/prompt/reload` | 热加载提示词文件 |

修改 `src/main/resources/prompts/` 下的文件后，调用 reload 接口即可生效，无需重启。

## 6. 动手实验

### 实验 1：修改 AI 角色

编辑 `system-prompt.txt`，将 AI 改为严格的合规审查员：

```
你是一个严格的金融合规审查员。在回答任何问题时，你必须：
1. 先判断问题是否涉及合规风险
2. 对有风险的操作给出明确的警告
3. 引用相关的监管规定
...
```

重启或调用 reload，观察 AI 回答风格的变化。

### 实验 2：添加 Few-shot 示例

在系统提示词中添加示例对话：

```
示例对话：
用户：贷款利率是多少？
助手：关于贷款利率，不同产品的利率范围如下：
- 产品A：年化 5.5%-8.0%
- 产品B：年化 7.2%-12.0%
请注意，实际利率会根据客户信用评估结果确定。请问您想了解哪个产品？
```

观察添加示例后，AI 输出格式是否更规范。

### 实验 3：A/B 测试

准备两版提示词，分别测试 10 个相同问题，对比：
- 回答准确率
- 回答相关性
- 输出格式规范性

## 7. 关键代码文件

| 文件 | 关注点 |
|------|--------|
| `service/prompt/PromptService.java` | 模板加载和热加载逻辑 |
| `config/PromptProperties.java` | 配置绑定 |
| `controller/PromptController.java` | Prompt 管理 API |
| `config/AiModelConfig.java` | prompt 注入 ChatClient |
| `src/main/resources/prompts/` | 模板文件目录 |
| `src/main/resources/application.yml` | `app.prompt.*` 配置节 |

---

## 来源文件：05-function-calling.md

> [!note] 原文链接
> [[05-function-calling.md]]

# 05 - Function Calling：让 AI 调用后端工具

## 1. 核心概念

### 什么是 Function Calling

Function Calling 让 LLM 能够**调用外部工具**。LLM 不直接执行代码，而是：
1. 分析用户意图
2. 决定是否需要调用工具
3. 选择调用哪个工具
4. 生成调用参数
5. 接收工具返回的结果
6. 将结果组织成自然语言回答

这是 AI Agent 系统的基础能力 -- Agent 的核心就是"感知环境 + 选择工具 + 执行行动"。

### 交互流程

```
用户: "帮我查一下贷款 LN001 的信息"
        │
        ▼
  ┌─────────────┐
  │  ChatClient  │ ──→ LLM 分析：需要查贷款数据
  └─────────────┘
        │ LLM 选择 loanQueryFunction，生成参数 {loanNo: "LN001"}
        ▼
  ┌──────────────────┐
  │ LoanQueryFunction │ ──→ SELECT * FROM loan WHERE loan_no = 'LN001'
  └──────────────────┘
        │ 返回贷款数据给 LLM
        ▼
  ┌─────────────┐
  │  ChatClient  │ ──→ LLM 组织自然语言回答
  └─────────────┘
        │
        ▼
"贷款 LN001 的信息：金额 50,000 元，状态为正常还款..."
```

### Spring AI 的实现方式

- 函数是标准的 Spring Bean，实现 `Function<I, O>` 接口
- 通过 `@Component` 注册到容器，通过 `@Description` 描述功能
- 在 ChatClient 构建时通过 `defaultFunctions()` 注册
- Spring AI 自动将函数描述发送给 LLM，LLM 自主决定调用

## 2. 三个函数详解

### 2.1 贷款查询函数 (LoanQueryFunction)

**功能**：根据贷款编号查询贷款详情

```java
@Component
@Description("查询贷款信息，包括贷款金额、状态、逾期天数等")
public class LoanQueryFunction implements Function<LoanQueryFunction.Request, LoanQueryFunction.Response> {

    private final LoanRepository loanRepository;

    @Override
    public Response apply(Request request) {
        Loan loan = loanRepository.findByLoanNo(request.loanNo());
        // 转换为 Response 返回
    }

    public record Request(String loanNo) {}
    public record Response(String loanNo, BigDecimal amount, String status, ...) {}
}
```

**关键点**：
- Request/Response 用 record 定义，结构清晰
- Spring AI 会自动将 record 的字段描述发送给 LLM
- LLM 根据字段名和函数描述决定是否调用

### 2.2 还款查询函数 (RepaymentQueryFunction)

**功能**：查询某笔贷款的还款记录

结构与 LoanQueryFunction 类似，查询的是 `repayment_record` 表。

### 2.3 风险评估函数 (RiskAssessmentFunction) -- 最值得学习

**功能**：综合贷款和还款数据，评估客户风险等级

这是三个函数中最复杂的，展示了 Function Calling 的真正价值 -- **让 LLM 调用包含复杂业务逻辑的函数**。

```
风险评估逻辑：
1. 查询贷款信息 -> 获取贷款金额、逾期天数
2. 查询还款记录 -> 获取逾期次数、待还款金额
3. 规则计算：
   - 逾期次数 >= 3        → HIGH
   - 逾期次数 >= 1        → MEDIUM
   - 贷款天数 > 180 且无逾期 → LOW
   - 其他                  → LOW（偏保守）
4. 生成风险描述和改进建议
```

**关键点**：
- 函数内部可以组合多个数据源（LoanRepository + RepaymentRecordRepository）
- 业务逻辑完全在 Java 代码中，LLM 只负责决定"是否调用"
- 返回结构化数据（riskLevel + description + recommendations），LLM 负责组织语言

## 3. 函数注册

在 `AiModelConfig.java` 中注册：

```java
@Bean
public ChatClient chatClient(ChatModel chatModel, ...) {
    return ChatClient.builder(chatModel)
        .defaultSystem(promptService.getSystemPrompt())
        .defaultAdvisors(
            new MessageChatMemoryAdvisor(chatMemory),
            new QuestionAnswerAdvisor(vectorStore)
        )
        .defaultFunctions(
            "loanQueryFunction",        // Bean 名称
            "repaymentQueryFunction",
            "riskAssessmentFunction"
        )
        .build();
}
```

`defaultFunctions()` 接收的是 Spring Bean 名称。Spring AI 会：
1. 从容器中获取对应的 Bean
2. 读取 `@Description` 注解的描述
3. 分析 `Function<Request, Response>` 的 record 字段
4. 将这些信息作为工具定义发送给 LLM API

## 4. 添加新函数的步骤

以添加"客户信息查询函数"为例：

### 步骤 1：创建实体和仓库

```java
@Entity
@Table(name = "customer")
public class Customer {
    @Id private Long id;
    private String name;
    private String idCard;
    private Integer creditScore;
}

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Customer findByIdCard(String idCard);
}
```

### 步骤 2：创建函数类

```java
@Component
@Description("查询客户信息，包括姓名、身份证号、信用评分")
public class CustomerQueryFunction implements Function<CustomerQueryFunction.Request, CustomerQueryFunction.Response> {

    private final CustomerRepository customerRepository;

    @Override
    public Response apply(Request request) {
        Customer customer = customerRepository.findByIdCard(request.idCard());
        return new Response(customer.getName(), customer.getIdCard(), customer.getCreditScore());
    }

    public record Request(@Description("客户身份证号") String idCard) {}
    public record Response(String name, String idCard, Integer creditScore) {}
}
```

### 步骤 3：注册到 ChatClient

在 `AiModelConfig.java` 的 `defaultFunctions()` 中添加 `"customerQueryFunction"`。

### 步骤 4：测试

在聊天界面输入："帮我查一下身份证号 320xxx 的客户信息"，AI 会自动调用新函数。

## 5. Function Calling 的局限和最佳实践

### 局限
- LLM 可能**错误调用**函数（选错函数或参数不对）
- 函数返回的数据量受 Token 限制
- 不适合实时性要求极高的场景（LLM 推理有延迟）

### 最佳实践
- 函数描述要**准确具体**，避免歧义
- Request 字段加 `@Description` 注解帮助 LLM 理解参数含义
- 函数返回**结构化数据**而非长文本，让 LLM 组织语言
- 对敏感操作（如修改数据）添加确认机制

## 6. 动手实验

### 实验 1：测试 Function Calling

在聊天界面依次输入，观察 AI 如何选择函数：

```
"帮我查一下贷款 LN001"               → loanQueryFunction
"LN001 的还款记录是什么"              → repaymentQueryFunction
"评估一下 LN001 的风险"               → riskAssessmentFunction
"贷款逾期了怎么办"                    → 不调用函数，用 RAG 知识回答
"查一下 LN001 的信息并评估风险"       → 可能连续调用多个函数
```

### 实验 2：添加新函数

按照第 4 节的步骤，添加一个 `CustomerQueryFunction`。

### 实验 3（挑战）：组合函数

创建一个 `LoanSummaryFunction`，内部同时调用 LoanRepository 和 RepaymentRecordRepository，返回贷款综合报告（本金、已还、待还、逾期情况）。

## 7. 关键代码文件

| 文件 | 关注点 |
|------|--------|
| `service/function/LoanQueryFunction.java` | 简单查询函数模板 |
| `service/function/RepaymentQueryFunction.java` | 列表查询函数 |
| `service/function/RiskAssessmentFunction.java` | 复杂业务逻辑函数 |
| `config/AiModelConfig.java` | 函数注册到 ChatClient |
| `model/entity/Loan.java` | 贷款实体 |
| `model/entity/RepaymentRecord.java` | 还款记录实体 |
| `repository/LoanRepository.java` | 贷款数据访问 |
| `repository/RepaymentRecordRepository.java` | 还款数据访问 |

---

## 来源文件：06-rag-basics.md

> [!note] 原文链接
> [[06-rag-basics.md]]

# 06 - RAG 基础：向量检索与知识库构建

## 1. 什么是 RAG

Retrieval-Augmented Generation，即"检索增强生成"。核心思路是先从知识库中检索相关文档，再将检索结果作为上下文喂给 LLM，让 LLM 基于真实数据生成回答。

**为什么需要 RAG：** LLM 的训练数据有截止日期，且不包含企业私有数据。直接问 LLM 企业内部问题，它要么不知道，要么编造答案。RAG 让 LLM 能基于你的真实文档回答问题。

**RAG vs Fine-tuning：**

| 对比项 | RAG | Fine-tuning |
|--------|-----|-------------|
| 知识更新 | 实时，改文件即可 | 需要重新训练 |
| 成本 | 低，只需维护向量库 | 高，需要 GPU 训练 |
| 可追溯性 | 可返回来源文档 | 黑盒 |
| 适用场景 | 事实性问答、知识库 | 风格调整、领域适配 |

## 2. RAG 核心流程

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ 用户提问  │ -> │ 向量检索  │ -> │ 组合提示词 │ -> │ LLM 生成  │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
                    ↑
              ┌──────────┐
              │ 知识库    │ <- 文档 -> 分块 -> 嵌入 -> 存储
              └──────────┘
```

1. **离线阶段（建库）：** 原始文档经过分块、嵌入，以向量形式存入 VectorStore。
2. **在线阶段（检索）：** 用户提问被转换为向量，与库中向量做相似度计算，返回最相关的文档片段。
3. **生成阶段：** 将检索到的文档片段注入 Prompt，LLM 基于这些上下文生成回答。

## 3. 向量嵌入 (Embedding)

将文本转换为高维数值向量（如 1024 维浮点数组），使语义相近的文本在向量空间中距离也相近。

本项目使用 **智谱 AI embedding-3 模型**，理由：

- 中文语义理解优于 OpenAI 的 text-embedding 模型
- 价格低，适合学习和开发阶段
- 无需额外配置，Spring AI 的 ZhipuAI starter 直接支持

在 Spring AI 中，通过 `EmbeddingModel` 接口统一调用，项目使用 `@Qualifier("zhiPuAiEmbeddingModel")` 指定。

## 4. 文档加载与处理 (VectorStoreConfig)

文档入库的完整流水线在 `VectorStoreConfig` 中实现：

### 4.1 流程概览

```
classpath:knowledge/*.txt + *.pdf
        ↓ 加载
   List<Document>
        ↓ 添加元数据 (filename, type, source, fileType)
   List<Document> (enriched)
        ↓ TokenTextSplitter 切分
   List<Document> (split)
        ↓ EmbeddingModel 向量化
   List<Document> (with embeddings)
        ↓ SimpleVectorStore.add()
   持久化到 JSON 文件
```

### 4.2 关键实现细节

**加载阶段：** 使用 `TextReader` 加载 TXT 文件，`PagePdfDocumentReader` 按页加载 PDF 文件。通过 `@Value("classpath:knowledge/*.txt")` 通配符自动扫描知识库目录。

**元数据标注：** 根据文件名自动分类，`determineDocumentType()` 方法按文件名关键字匹配类型：

- 文件名含 `rules` 或 `rule` -> 类型 "规则"
- 文件名含 `product` 或 `manual` -> 类型 "产品"
- 文件名含 `risk` 或 `control` -> 类型 "风控"
- 文件名含 `contract` 或 `template` -> 类型 "合同"

**文档切分：** 使用 Spring AI 的 `TokenTextSplitter`，按 token 数量切分，确保每个片段不超过模型上下文限制。

**持久化：** 使用 `SimpleVectorStore`，向量数据保存为 JSON 文件（默认路径 `./data/vector-store.json`），启动时检测文件是否存在，存在则直接加载，否则重新构建。

### 4.3 核心代码结构

```java
@Bean
public VectorStore vectorStore(
    @Qualifier("zhiPuAiEmbeddingModel") EmbeddingModel embeddingModel
) {
    SimpleVectorStore vectorStore = new SimpleVectorStore(embeddingModel);
    if (vectorStoreFile.exists()) {
        vectorStore.load(vectorStoreFile);       // 已有缓存，直接加载
    } else {
        List<Document> docs = loadAndSplitDocuments();  // 加载、分块
        vectorStore.add(docs);                          // 向量化并存入
        vectorStore.save(vectorStoreFile);              // 持久化
    }
    return vectorStore;
}
```

## 5. 知识库文件结构

```
src/main/resources/knowledge/
├── product_manual.txt        # 产品手册 (type=产品)
├── loan_business_rules.txt   # 业务规则 (type=规则)
├── risk_control_guide.txt    # 风控指南 (type=风控)
└── 借款合同.pdf              # 合同模板 (type=合同)
```

文件名决定了元数据中的 `type` 字段，检索时可通过该字段做过滤。

## 6. 基础向量检索

### 6.1 两种使用方式

**方式一：QuestionAnswerAdvisor（自动注入）**

Spring AI 提供的 Advisor，自动将检索结果注入到 Prompt 中，适合直接在聊天流程中使用。

**方式二：手动检索**

直接调用 `VectorStore.similaritySearch()`，获取原始文档列表，适合需要对检索结果做二次处理的场景。

### 6.2 配置参数

在 `application.yml` 的 `app.chat.rag` 段配置，对应 `RagProperties` 类：

```yaml
app:
  chat:
    rag:
      similarity-threshold: 0.2   # 相似度阈值，ZhipuAI 建议设 0.2
      top-k: 5                    # 返回 Top-K 文档
```

`similarityThreshold` 的值设为 0.2 而非更高的原因：ZhipuAI 对"长文本片段 vs 短查询"的相似度分数通常偏低（0.2-0.3 范围），阈值过高会导致召回不足。

### 6.3 SearchRequest 构建模式

```java
SearchRequest request = SearchRequest.query(query)
    .withTopK(topK)
    .withSimilarityThreshold(threshold);
List<Document> results = vectorStore.similaritySearch(request);
```

## 7. API 接口

基础检索接口：

```
GET /api/rag/search?query=xxx&type=规则&topK=5
```

- `query`: 查询文本
- `type`: 可选，按文档类型过滤（规则/产品/风控/合同）
- `topK`: 可选，返回数量，默认 5

该接口内部会先调用 `QueryExpansionService` 扩展查询，再执行向量检索，提升召回率。

## 8. 动手实验

1. **添加新知识文档：** 在 `src/main/resources/knowledge/` 目录下放入新的 TXT 或 PDF 文件，删除 `./data/vector-store.json`，重启应用观察新文档是否被正确加载和检索。
2. **调整相似度阈值：** 将 `app.chat.rag.similarity-threshold` 从 0.2 改到 0.5 或 0.1，对比同一查询的检索结果数量和相关性变化。
3. **调整分块大小：** `TokenTextSplitter` 默认参数切分，可以尝试自定义实例化时传入不同的 `chunkSize`，观察小块 vs 大块对检索精度的影响。

## 9. 关键代码文件

| 文件 | 说明 |
|------|------|
| `src/main/java/.../config/VectorStoreConfig.java` | 向量库初始化、文档加载与切分 |
| `src/main/java/.../config/RagProperties.java` | RAG 配置属性（阈值、TopK） |
| `src/main/java/.../service/rag/QueryExpansionService.java` | 查询扩展（同义词） |
| `src/main/resources/knowledge/` | 知识库文件目录 |
| `src/main/resources/application.yml` | `app.chat.rag` 配置段 |

---

## 来源文件：07-rag-advanced.md

> [!note] 原文链接
> [[07-rag-advanced.md]]

# 07 - RAG 进阶：查询优化、混合检索与智能管道

## 1. 为什么需要进阶 RAG

基础向量检索存在以下局限：

- **关键词匹配弱：** 用户搜 "M1 阶段"，向量检索可能返回与 "第一阶段" 相关的内容，而不是包含精确字符串 "M1" 的文档。
- **短查询语义模糊：** 三个字的查询生成的向量信息量太少，相似度计算不够准确。
- **多义词问题：** "黑名单"在不同上下文中含义不同。
- **上下文缺失：** 多轮对话中 "它支持提前还款吗" 的 "它" 无法被检索系统理解。

解决思路：查询预处理 + 多路召回 + 精排，形成完整的 RAG 管道。

## 2. 查询预处理

### 2.1 查询扩展 (QueryExpansionService)

**目的：** 用同义词和专业术语扩展原始查询，提升召回率。

**实现方式：** 基于静态字典的扩展，不调用 LLM，速度极快（毫秒级）。

**字典结构：** 两套字典互补：

- `SYNONYMS`：口语词 -> 正式术语列表（如 "黑名单" -> ["征信黑名单", "不良记录", "失信名单", "失信被执行人"]）
- `TECHNICAL_TERMS`：专业术语 -> 口语表达（如 "M1" -> ["第一阶段", "1-30天"]，"征信上报" -> ["黑名单", "不良记录"]）

**扩展逻辑：**

```
原始查询: "黑名单客户怎么处理"
  ↓ 匹配 SYNONYMS["黑名单"]
扩展后: "黑名单客户怎么处理 OR 征信黑名单 OR 不良记录 OR 征信不良 OR 失信名单 OR 失信被执行人"
```

关键设计点：

- 使用 `LinkedHashSet` 保持插入顺序，原始查询始终排在最前面
- 使用 `OR` 连接所有扩展词，在向量检索中覆盖更多语义空间
- 逆向字典 `TECHNICAL_TERMS` 处理用户输入专业术语时也能匹配到口语化的文档内容

**调试接口：** `QueryExpansionService.getExpansionDetail(query)` 返回匹配了哪些同义词和专业术语。

### 2.2 查询重写 (QueryRewriteService)

**目的：** 解决多轮对话中的指代消解和上下文缺失问题。

**两阶段过滤机制：**

**第一阶段 -- 规则检查（快速，本地判断）：** `needsRewrite()` 方法检测以下模式：

- 包含代词：它、这个、那个、它们、这些、那些
- 省略主语：以 "那"、"那么"、"然后"、"接着" 开头
- 过于简短：少于 5 个字且不含问号
- 需要上下文：包含 "怎么办"、"怎么样"、"如何"

如果都不命中，直接返回原始查询，避免不必要的 LLM 调用。

**第二阶段 -- LLM 重写（慢速，远程调用）：** 将对话历史和当前问题发送给 LLM，Prompt 要求 LLM 执行四项任务：

1. 指代消解：将代词替换为具体实体
2. 补全省略：根据上下文补充完整的问题
3. 口语规范化：口语表达转书面语
4. 保持原意：不改变用户原本想问的问题

LLM 只返回重写后的查询文本，不做任何解释。

**优雅降级：** LLM 调用失败时（网络异常、超时等），返回原始查询，不影响主流程。

**调试接口：**

```
GET /api/chat/query-rewrite?query=它支持提前还款吗&conversationId=xxx
```

返回 `RewriteDetail`，包含原始查询、重写结果、是否改变、改变原因。

## 3. 多路召回

### 3.1 BM25 关键词检索 (BM25Service)

BM25 是基于 TF-IDF 改进的经典信息检索算法，本项目的实现要点：

**算法参数：**

- `k1 = 1.2`：控制词频饱和度，值越大对高频词越敏感
- `b = 0.75`：控制文档长度归一化，值越大对长文档的惩罚越强

**中文分词策略：** 采用 n-gram 方式，无需引入 jieba 等外部分词库：

- 对中文文本生成 bigram（相邻两字组合）和 trigram（相邻三字组合）
- 同时保留单字作为最细粒度匹配
- 英文和数字按空格/标点分割后转小写

**为什么需要 BM25：** 向量检索对精确关键词匹配较弱。例如查询 "CUST001"，向量检索可能找不到包含精确编号 "CUST001" 的文档，而 BM25 能精确匹配。两者互补。

**索引构建：** 在服务初始化时，通过 `vectorStore.similaritySearch` 获取所有文档，构建词频统计和文档频率统计。

### 3.2 混合检索 (HybridSearchService)

**RRF (Reciprocal Rank Fusion) 算法** 融合向量检索和 BM25 的结果：

```
final_score(doc) = sum( 1 / (k + rank_i(doc)) )  for each retrieval method
```

其中 `k = 60` 是平滑参数，防止排名靠前的文档分数过大。

**算法步骤：**

1. 分别执行向量检索和 BM25 检索，各自返回 Top-K 结果
2. 为每种检索结果建立排名（1-based）
3. 对每个文档计算 RRF 分数：`1 / (60 + rank)`
4. 同一文档在两种检索中都出现时，RRF 分数累加
5. 按最终分数降序排序，返回 Top-K

**优势：**

- 不需要归一化不同检索方法的分数（向量相似度 vs BM25 分数量级完全不同）
- 对异常值不敏感
- 实现简单，效果好

**API：**

```
GET /api/rag/hybrid-search?query=xxx&topK=5
```

返回结果包含每个文档的 RRF 分数、向量排名和 BM25 排名，便于调试分析。

## 4. 精排

### 4.1 LLM 重排序 (RerankService)

采用 **Retrieve-then-Rerank** 模式：

**流程：**

```
向量检索(低阈值, Top-20)
        ↓ 召回候选文档
所有候选文档 + query
        ↓ 发送给 LLM
LLM 对每个文档打分(0.0-1.0)
        ↓ 按新分数排序
返回 Top-K
```

**粗筛阶段：** 向量检索使用 `similarityThreshold = 0.0`（无阈值过滤），召回 `RERANK_TOP_N = 20` 个候选文档，尽可能多召回。

**LLM 打分：** 将所有候选文档内容截取前 200 字符，连同查询一起发送给 LLM。Prompt 要求 LLM 按以下标准打分：

- 1.0：完全相关，直接回答了查询问题
- 0.7-0.9：高度相关
- 0.4-0.6：部分相关
- 0.1-0.3：弱相关
- 0.0：不相关

LLM 返回格式为 `[文档编号] 分数`，如 `[1] 0.85`，通过正则解析。

**优雅降级：** LLM 调用失败或返回格式无法解析时，回退到向量检索的原始分数排序，返回结果中 `reranked = false` 标记。

**API：**

```
GET /api/rag/rerank-search?query=xxx&topK=5
```

## 5. 智能 RAG 管道 (RagPipelineService)

自动组合以上所有技术，形成 4 阶段管道：

```
阶段1: 查询重写 (QueryRewriteService)
  ↓
阶段2: 查询扩展 (QueryExpansionService)
  ↓
阶段3: 策略选择 (自动/固定)
  ↓
阶段4: 执行检索 (VectorStore / HybridSearch / Rerank)
```

### 5.1 策略选择逻辑

管道支持 4 种检索策略：

| 策略枚举 | 含义 | 触发条件 |
|---------|------|---------|
| `VECTOR_ONLY` | 纯向量检索 | 默认策略 |
| `VECTOR_WITH_RERANK` | 向量 + LLM 重排 | 问题型查询（含"如何"、"什么"等） |
| `HYBRID_SEARCH` | 向量 + BM25 混合 | 包含大写字母、数字等专有名词 |
| `HYBRID_WITH_RERANK` | 混合 + 重排 | 长查询（超过 `complexQueryLength` 阈值） |

**自动模式：** `autoMode = true` 时，根据查询特征自动选择策略。特征检测包括：

- `isQuestionQuery()`：查询包含配置的问题词（"怎么办"、"如何"、"怎么"、"为什么"等）
- `hasTechnicalTerms()`：包含大写字母、数字、或大写+数字组合
- `isLongQuery()`：查询长度超过 `complexQueryLength`（默认 10 字）

**固定模式：** `autoMode = false` 时，根据 `enableHybridSearch` 和 `enableRerank` 的开关组合确定策略。

### 5.2 配置开关

`RagPipelineProperties` 对应 `application.yml` 的 `app.chat.rag.pipeline` 段：

```yaml
app:
  chat:
    rag:
      pipeline:
        enable-query-rewrite: true      # 启用查询重写
        enable-query-expansion: true    # 启用查询扩展
        enable-hybrid-search: false     # 启用混合检索（默认关闭，需更多计算）
        enable-rerank: false            # 启用重排序（默认关闭，速度慢）
        auto-mode: true                 # 自动选择策略
        complex-query-length: 10        # 复杂查询长度阈值
        question-words:                 # 问题词列表
          - 怎么办
          - 如何
          - 怎么
          - 为什么
          - 是什么
          - 哪些
          - 如何处理
```

每个阶段可通过开关独立控制，关闭的阶段直接跳过。

### 5.3 管道执行结果

`PipelineResult` 包含完整的信息链路：

- `originalQuery`：用户原始输入
- `rewrittenQuery`：重写后的查询
- `expandedQuery`：扩展后的查询
- `strategy`：选择的检索策略枚举
- `documents`：最终检索到的文档列表

可通过 `getTransformationSummary()` 获取查询变换的摘要文本。

**API：**

```
GET /api/rag/pipeline?query=xxx&conversationId=xxx&topK=5
```

## 6. RAG 评估 (RagEvaluationService)

### 6.1 为什么需要评估

没有量化指标就无法判断优化方向。评估回答两个问题：找得全不全（召回率）？找得准不准（精确率）？

### 6.2 五个核心指标

| 指标 | 公式 | 含义 |
|------|------|------|
| **Recall（召回率）** | 检索到的相关文档数 / 所有相关文档数 | 应该找到的文档，找到了多少 |
| **Precision（精确率）** | 检索到的相关文档数 / 检索到的总文档数 | 找到的文档中，有多少是真正相关的 |
| **F1** | 2 * P * R / (P + R) | 召回率和精确率的调和平均，综合指标 |
| **MRR** | 1 / 第一个相关文档的排名 | 第一个正确结果排得有多靠前 |
| **NDCG** | DCG / IDCG | 考虑排序位置的指标，相关文档排在越前面分数越高 |

**使用方式：** 人工标注一批"查询 -> 相关文档ID"的测试集，调用评估接口自动计算指标。

**API：**

```
POST /api/rag/evaluate
Body: { "query": "xxx", "relevantDocIds": ["id1", "id2"], "topK": 5 }
```

支持批量评估（`batchEvaluate`），对多个查询计算平均指标。

## 7. 检索策略对比总结

| 策略 | 适用场景 | 优点 | 缺点 |
|------|---------|------|------|
| 纯向量 (VECTOR_ONLY) | 语义匹配、同义词查询 | 理解语义，实现简单 | 精确关键词匹配弱 |
| 向量+重排 (VECTOR_WITH_RERANK) | 需要精确回答的问题型查询 | 高精确率，LLM 深度理解 | 慢，需要额外 LLM 调用 |
| 混合检索 (HYBRID_SEARCH) | 通用场景，专有名词 | 覆盖全面，向量+关键词互补 | 计算量比纯向量大 |
| 混合+重排 (HYBRID_WITH_RERANK) | 复杂长查询 | 最佳效果 | 最慢，多路召回+LLM 打分 |

实际选择建议：先用纯向量跑通流程，遇到召回不足加查询扩展，遇到关键词匹配问题加混合检索，对精确率有要求加重排序。

## 8. 动手实验

1. **策略对比：** 用同一个查询（如 "M1 阶段的征信上报流程"）分别调用 `/search`、`/hybrid-search`、`/rerank-search`、`/pipeline` 接口，对比结果差异。
2. **同义词扩展：** 在 `QueryExpansionService` 的 `SYNONYMS` 字典中添加新的映射，重启后观察包含该词的查询召回率变化。
3. **量化评估：** 准备 5-10 个测试查询，标注每个查询的相关文档 ID，调用 `/evaluate` 接口，对比不同策略的 Recall 和 Precision。
4. **BM25 参数调优：** 修改 `BM25Service` 中的 `K1`（默认 1.2）和 `B`（默认 0.75），观察关键词检索效果的变化。
5. **管道开关：** 逐个开关 `enable-query-rewrite`、`enable-query-expansion`、`enable-hybrid-search`、`enable-rerank`，观察每个组件对最终结果的影响。

## 9. 关键代码文件

| 文件 | 说明 |
|------|------|
| `src/main/java/.../service/rag/QueryRewriteService.java` | 查询重写（指代消解、上下文补全） |
| `src/main/java/.../service/rag/QueryExpansionService.java` | 查询扩展（同义词、专业术语） |
| `src/main/java/.../service/rag/BM25Service.java` | BM25 关键词检索 |
| `src/main/java/.../service/rag/HybridSearchService.java` | 混合检索（RRF 融合） |
| `src/main/java/.../service/rag/RerankService.java` | LLM 重排序 |
| `src/main/java/.../service/rag/RagPipelineService.java` | 智能 RAG 管道（组合以上所有） |
| `src/main/java/.../service/rag/RagEvaluationService.java` | RAG 评估（Recall、Precision、F1、MRR、NDCG） |
| `src/main/java/.../config/RagPipelineProperties.java` | 管道配置开关 |

---

## 来源文件：08-cost-and-observability.md

> [!note] 原文链接
> [[08-cost-and-observability.md]]

# 08 - 成本管理与可观测性：AOP 日志、成本统计与缓存策略

## 1. 为什么需要关注 AI 调用成本

- LLM API 按 Token 计费，不加控制会产生高额费用
- 生产环境必须监控：每次调用成本、累计成本、异常调用
- 优化手段：缓存、合理选择模型、控制上下文长度

## 2. AOP 调用日志 (AiCallLoggerAspect)

使用 Spring AOP `@Around` 切面拦截所有 `ChatModel.call()` 和 `EmbeddingModel.embed()` 调用。

核心流程：
1. 记录开始时间，生成 sessionId
2. 从目标对象推断模型名称和提供商
3. 调用 `joinPoint.proceed()` 执行实际方法
4. 成功时提取 Token 信息、计算成本、保存日志
5. 失败时记录错误信息，仍然保存日志，然后抛出原始异常

```java
@Around("execution(* org.springframework.ai.chat.model.ChatModel.call(..))")
public Object logChatCall(ProceedingJoinPoint joinPoint) throws Throwable {
    return logAiCall(joinPoint, ModelType.CHAT);
}

@Around("execution(* org.springframework.ai.embedding.EmbeddingModel.embed(..))")
public Object logEmbeddingCall(ProceedingJoinPoint joinPoint) throws Throwable {
    return logAiCall(joinPoint, ModelType.EMBEDDING);
}
```

关键设计决策：
- **日志保存失败不影响业务**：`saveCallLog()` 内部 try-catch 吞掉异常，只打印 error 日志
- **模型名推断**：通过类名（如 `OpenAi`、`ZhiPuAi`）推断模型和提供商，映射到定价表
- **Token 估算**：当 API 响应中缺少 usage 数据时，按中文字符数/2 粗略估算

## 3. 成本计算 (AiCostCalculator)

按模型定价表计算每次调用费用，输入/输出 Token 分别计价，单位为元/千 Token。

```java
private static final Map<String, ModelPricing> PRICING_TABLE = new HashMap<>();

static {
    PRICING_TABLE.put("deepseek-chat", new ModelPricing(
        new BigDecimal("0.001"),  // 输入
        new BigDecimal("0.002")   // 输出
    ));
    PRICING_TABLE.put("embedding-2", new ModelPricing(
        new BigDecimal("0.0007"),
        new BigDecimal("0.0007")
    ));
}
```

计算公式：

```
输入成本 = (promptTokens / 1000) * inputPrice
输出成本 = (completionTokens / 1000) * outputPrice
总成本 = 输入成本 + 输出成本（保留6位小数）
```

定价参考（以实际官网为准）：

| 模型 | 输入价格(元/千Token) | 输出价格(元/千Token) |
|------|-------------------|-------------------|
| deepseek-chat | 0.001 | 0.002 |
| embedding-2 | 0.0007 | 0.0007 |

未知模型返回 BigDecimal.ZERO，不会报错。添加新模型只需在 static 块中 put 一行。

## 4. 成本统计服务 (AiCostStatisticsService)

聚合查询支持四个时间维度：今日、本周、本月、自定义范围。

统计内容：
- 总调用次数、成功次数、成功率
- 总成本
- 模型使用分布：每个模型的调用次数、Token 用量、成本
- 每日趋势：按天的成本和调用次数

返回数据结构：

```java
public record CostStats(
    LocalDateTime startTime,
    LocalDateTime endTime,
    BigDecimal totalCost,
    long totalCalls,
    long successCalls,
    BigDecimal successRate,          // 百分比
    List<ModelUsage> modelUsages,
    List<DailyCost> dailyCosts
) {}

public record ModelUsage(
    String modelName, String provider,
    long callCount, long totalTokens, BigDecimal totalCost
) {}

public record DailyCost(
    LocalDate date, BigDecimal cost, long callCount
) {}
```

底层依赖 `AiCallLogRepository` 的 JPQL 聚合查询，按模型分组统计和按日期分组统计各一条查询。

## 5. 成本管理前端

- `cost.html`：成本看板页面，包含时间段选择器、统计卡片、每日趋势表格、模型分布
- `cost.js`：`CostManager` 类，通过 fetch 调用 `/api/cost/*` 接口获取数据
- `cost.css`：独立样式

页面结构：
1. **时间段选择**：今日 / 本周 / 本月 / 自定义（自定义弹出日期选择器）
2. **统计卡片**：总调用次数、总成本、成功/总调用比、成功率
3. **每日成本趋势表格**：按天展示成本和调用次数
4. **模型分布**：按模型展示调用次数、Token 用量、成本

从聊天页（index.html）导航栏可直接跳转到成本页面。

## 6. 定时任务 (DailyCostStatisticsScheduler)

使用 `@Scheduled` 注解实现两个定时任务：

| 任务 | cron 表达式 | 说明 |
|------|-----------|------|
| 每日统计 | `0 0 1 * * ?`（凌晨1点） | 汇总昨日各模型的调用次数、Token、成本，输出日志 |
| 成本预警 | `0 */10 * * * ?`（每10分钟） | 检查今日累计成本是否超过阈值（默认100元），超限打印 warn 日志 |

预警阈值硬编码为 `new BigDecimal("100")`，可根据需要调整或改为配置项。

扩展方向：将统计结果持久化到 `ai_daily_cost_stats` 表，或接入邮件/钉钉/企微通知。

## 7. 多级缓存策略

### 7.1 缓存架构

三个独立的 `CacheManager`，基于 Caffeine，各有不同的 TTL 和容量：

| 缓存类型 | Bean 名称 | TTL | 最大条目 | 适用场景 |
|---------|----------|-----|---------|---------|
| AI 响应缓存 | aiResponseCacheManager | 30 分钟 | 1000 | 重复问题的缓存命中 |
| 向量检索缓存 | vectorSearchCacheManager | 1 小时 | 500 | 相同查询的检索结果 |
| 函数调用缓存 | functionCallCacheManager | 10 分钟 | 2000 | 频繁查询的贷款信息 |
| 默认缓存 | defaultCacheManager (@Primary) | 30 分钟 | 10000 | 通用场景 |

### 7.2 Caffeine Cache 特点

- 本地内存缓存，零网络开销
- 支持 TTL（expireAfterWrite）、容量上限（maximumSize）、LRU 淘汰
- 适合单机部署场景，多实例部署需替换为 Redis

### 7.3 缓存 Key 设计

- AI 响应：`#question` 或 `'system:' + #systemPrompt + '|question:' + #question`
- 向量检索：`#query + '_' + #topK + '_' + #threshold`
- 函数调用：根据具体函数参数设计

通过 `cacheManager` 参数指定使用哪个 CacheManager：

```java
@Cacheable(value = "qaCache", key = "#question", cacheManager = "aiResponseCacheManager")
public String ask(String question) { ... }

@Cacheable(value = "vectorSearch", key = "#query + '_' + #topK + '_' + #threshold",
           cacheManager = "vectorSearchCacheManager")
public List<Document> similaritySearch(String query, int topK, double threshold) { ... }
```

缓存失效使用 `@CacheEvict`，支持单条清除和全部清除：

```java
@CacheEvict(value = "qaCache", key = "#question")
public void evictQuestion(String question) { ... }

@CacheEvict(value = "qaCache", allEntries = true)
public void evictAllQuestions() { ... }
```

## 8. AiCallLog 数据模型

```java
@Entity
@Table(name = "ai_call_log", indexes = {
    @Index(name = "idx_session", columnList = "session_id"),
    @Index(name = "idx_user", columnList = "user_id"),
    @Index(name = "idx_model", columnList = "model_name"),
    @Index(name = "idx_created", columnList = "created_time"),
    @Index(name = "idx_provider", columnList = "provider")
})
public class AiCallLog {
    private Long id;
    private String sessionId;
    private String userId;
    private String modelName;
    private String modelType;      // CHAT / EMBEDDING
    private String provider;       // OPENAI / ZHIPUAI / ANTHROPIC
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private BigDecimal cost;       // 本次调用成本（元），精度 10,6
    private Long latencyMs;        // 响应耗时（毫秒）
    private String status;         // SUCCESS / FAILURE / TIMEOUT
    private String errorMessage;
    private LocalDateTime createdTime;  // @PrePersist 自动填充
}
```

五个索引覆盖了按会话查询、按用户查询、按模型查询、按时间范围查询、按提供商查询的场景。

## 9. API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/cost/today` | 今日成本统计 |
| GET | `/api/cost/week` | 本周成本统计 |
| GET | `/api/cost/month` | 本月成本统计 |
| GET | `/api/cost/range?start=&end=` | 自定义时间范围（ISO DATE_TIME 格式） |

所有接口返回 `CostStats` record，包含统计概要、模型使用分布、每日成本趋势。

## 10. 动手实验

1. 发送多次相同的聊天消息，观察缓存命中率（通过日志或 AOP 记录）
2. 修改 `AiCostCalculator` 的 `PRICING_TABLE`，添加新模型定价，观察成本计算变化
3. 添加一个新的统计维度：在 Repository 中按 `sessionId` 分组统计成本
4. 实现成本预警：当单次调用成本超过 1 元时，在 AOP 切面中打印 warn 日志

## 11. 关键代码文件

| 文件 | 说明 |
|------|------|
| `src/main/java/.../interceptor/AiCallLoggerAspect.java` | AOP 调用日志切面 |
| `src/main/java/.../service/cost/AiCostCalculator.java` | 成本计算器 |
| `src/main/java/.../service/cost/AiCostStatisticsService.java` | 成本统计服务 |
| `src/main/java/.../controller/AiCostController.java` | 成本统计 API |
| `src/main/java/.../scheduled/DailyCostStatisticsScheduler.java` | 定时统计与预警 |
| `src/main/java/.../config/CacheConfig.java` | Caffeine 缓存配置 |
| `src/main/java/.../service/cache/CachedChatService.java` | 带缓存的问答服务 |
| `src/main/java/.../service/cache/CachedVectorSearchService.java` | 带缓存的向量检索 |
| `src/main/java/.../model/entity/AiCallLog.java` | 调用日志实体 |
| `src/main/java/.../repository/AiCallLogRepository.java` | 日志数据访问层 |
| `src/main/resources/static/cost.html` | 成本看板页面 |
| `src/main/resources/static/js/cost.js` | 成本看板逻辑 |
| `src/main/resources/static/css/cost.css` | 成本看板样式 |

---

## 来源文件：09-ai-form-filling-demo.md

> [!note] 原文链接
> [[09-ai-form-filling-demo.md]]

# AI 表单自动填充 Demo

> 通过这个实战 demo，理解 AI 如何根据文档自动填充多标签页表单，并实现人工介入打断

---

## 📋 目录

1. [功能介绍](#功能介绍)
2. [核心概念](#核心概念)
3. [技术架构](#技术架构)
4. [代码实现](#代码实现)
5. [AI Prompt 工程](#ai-prompt-工程)
6. [运行体验](#运行体验)
7. [关键知识点](#关键知识点)

---

## 功能介绍

### 业务场景

用户需要填写一份复杂的贷款申请表单，表单分为 3 个标签页：
- **基本信息**：姓名、身份证、电话、地址、职业、收入
- **贷款信息**：贷款类型、金额、期限、用途、还款来源
- **担保信息**：担保方式、抵押物描述、估值、保证人信息

传统的做法是用户逐个手动填写，费时费力且容易出错。

### AI 解决方案

用户只需提供一份自然语言描述的贷款需求文档，AI 即可：
1. 理解文档内容
2. 提取结构化信息
3. 自动填充到对应表单字段
4. 流式展示填充过程
5. 支持人工暂停介入修改

### 演示效果

```
用户操作：
1. 选择"张三个人贷款申请"文档
2. 点击"开始 AI 填充"

AI 处理过程（可视化显示）：
> 正在读取文档内容...
> ✅ 文档读取成功，共 328 字符
> 🔍 正在使用 AI 智能提取信息...
> 📤 向 AI 发送请求...
> 📥 收到 AI 响应，正在解析...
> ✅ AI 已完成信息提取，开始填充表单...
> 📑 切换到标签页：基本信息
> ✏️ 填充字段 [申请人姓名]: 张三
> ✏️ 填充字段 [身份证号]: 110101199001011234
> ...（逐步填充所有字段）
> 🎉 所有字段填充完成！
```

---

## 核心概念

### 1. SSE (Server-Sent Events)

服务端推送技术，允许服务器持续向客户端发送数据流。

```javascript
// 前端建立 SSE 连接
const eventSource = new EventSource('/api/loan-form/fill-stream');

eventSource.onmessage = (event) => {
    const data = JSON.parse(event.data);
    // 处理不同类型的消息：thinking, progress, fill_field 等
};
```

```java
// 后端流式发送
emitter.send(SseEmitter.event().data(jsonData));
```

### 2. Prompt Engineering

通过精心设计的 System Prompt 和 User Prompt，让 AI 输出结构化的 JSON 数据。

```
System Prompt: 定义 AI 角色和输出格式
User Prompt:   提供具体的文档内容
```

### 3. 数据映射

将 AI 提取的数据映射到前端表单字段，实现自动填充。

---

## 技术架构

```
┌─────────────────────────────────────────────────────────────────┐
│                          浏览器端                                │
│  ┌──────────────┐    SSE    ┌────────────────────────────────┐ │
│  │ loan-form.js │ ←──────── │ 事件监听与UI更新                │ │
│  └──────────────┘            │ - 接收 thinking 事件           │ │
│                              │ - 接收 fill_field 事件         │ │
│                              │ - 高亮当前填充字段              │ │
│                              └────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                  ↕ SSE
┌─────────────────────────────────────────────────────────────────┐
│                         Spring Boot 服务端                      │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ LoanFormController (/api/loan-form/fill-stream)          │  │
│  │  - 创建 SseEmitter                                        │  │
│  │  - 异步处理文档                                            │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              ↓                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ LoanFormService.processDocumentAndFill()                  │  │
│  │                                                            │  │
│  │  1. 读取示例文档                                           │  │
│  │  2. 构建 Prompt → 调用 AI                                  │  │
│  │  3. 解析 AI 返回的 JSON                                    │  │
│  │  4. 逐步发送 SSE 事件：                                     │  │
│  │     - switch_tab: 切换标签页                               │  │
│  │     - fill_field: 填充单个字段                             │  │
│  │     - progress: 更新进度                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              ↓                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Spring AI + ChatModel                                     │  │
│  │  - 发送结构化 Prompt                                       │  │
│  │  - 返回 JSON 格式的提取结果                                │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 代码实现

### 文件结构

```
src/main/java/com/example/aidevelop/
├── controller/
│   └── LoanFormController.java          # SSE 端点
├── service/
│   └── LoanFormService.java             # 核心业务逻辑
└── model/dto/loanform/
    ├── FieldFillRequest.java            # 字段填充请求
    └── LoanFormData.java                # 表单数据模型

src/main/resources/static/
├── loan-form.html                       # 页面结构
├── css/loan-form.css                    # 样式
└── js/loan-form.js                      # 前端逻辑
```

### 1. 后端 SSE 端点

**文件**: `LoanFormController.java`

```java
@GetMapping(value = "/fill-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter fillFormStream(@RequestParam String docId) {
    SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

    // 异步处理，避免阻塞
    new Thread(() -> {
        try {
            loanFormService.processDocumentAndFill(docId, emitter);
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }).start();

    return emitter;
}
```

### 2. AI 提取核心逻辑

**文件**: `LoanFormService.java`

```java
private LoanFormData extractFieldsWithAI(String document, SseEmitter emitter) {
    // System Prompt: 定义 AI 角色和输出格式
    String systemPrompt = """
        你是一个专业的贷款申请信息提取助手。
        请严格按照以下 JSON 格式返回：
        {
          "applicantName": "申请人姓名",
          "idNumber": "身份证号",
          ...
        }
        """;

    // User Prompt: 提供文档内容
    String userPrompt = "请从以下文档中提取信息：\n" + document;

    // 调用 AI
    Prompt prompt = new Prompt(
        new SystemMessage(systemPrompt),
        new UserMessage(userPrompt)
    );

    String response = chatModel.call(prompt).getResult().getOutput().getContent();

    // 解析 JSON
    return objectMapper.readValue(response, LoanFormData.class);
}
```

### 3. 前端 SSE 接收

**文件**: `loan-form.js`

```javascript
function startAiFill() {
    const url = `/api/loan-form/fill-stream?docId=${documentSelect.value}`;
    eventSource = new EventSource(url);

    eventSource.onmessage = (event) => {
        if (isPaused) return; // 支持暂停

        const data = JSON.parse(event.data);
        handleAiMessage(data);
    };
}

function handleAiMessage(data) {
    switch (data.type) {
        case 'thinking':
            addLog(data.content, 'thinking');
            break;
        case 'switch_tab':
            switchTab(data.tabId);
            break;
        case 'fill_field':
            fillField(data.tabId, data.fieldName, data.value);
            break;
        case 'complete':
            stopFilling();
            break;
    }
}
```

### 4. 字段填充动画

**文件**: `loan-form.css`

```css
/* 填充中状态 - 呼吸动画 */
.form-group.filling input {
    border-color: var(--warning-color);
    animation: pulse 1.5s infinite;
}

/* 已填充状态 */
.form-group.filled input {
    border-color: var(--success-color);
    background: #f0fdf4;
}

@keyframes pulse {
    0%, 100% { box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.1); }
    50% { box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.3); }
}
```

---

## AI Prompt 工程

### System Prompt 设计要点

1. **明确角色定位**
   ```
   你是一个专业的贷款申请信息提取助手
   ```

2. **指定输出格式**
   ```
   请严格按照以下 JSON 格式返回
   ```

3. **定义字段映射规则**
   ```
   loanType 映射：个人消费=personal, 企业经营=business...
   ```

4. **处理缺失信息**
   ```
   如果文档中没有某个字段的信息，填空字符串或 0
   ```

5. **约束输出内容**
   ```
   只返回 JSON，不要有其他解释文字
   ```

### User Prompt 设计

```
请从以下贷款申请文档中提取信息：

{文档内容}

请返回 JSON 格式的结构化数据。
```

---

## 运行体验

### 1. 启动应用

```bash
mvn spring-boot:run
```

### 2. 访问页面

```
http://localhost:8080/loan-form.html
```

### 3. 操作步骤

| 步骤 | 操作 | 观察效果 |
|------|------|----------|
| 1 | 从下拉框选择文档 | 文档预览按钮可用 |
| 2 | 点击"预览文档" | 显示完整文档内容 |
| 3 | 点击"开始 AI 填充" | AI 思考日志开始滚动 |
| 4 | 观察 AI 处理 | 进度条逐步更新 |
| 5 | 等待填充完成 | 所有字段自动填充 |
| 6 | 可随时点击"暂停" | 填充停止，可手动编辑 |

### 4. 暂停/继续功能

```javascript
function togglePause() {
    isPaused = !isPaused;
    pauseBtn.textContent = isPaused ? '▶️ 继续' : '⏸️ 暂停';

    if (isPaused) {
        addLog('⏸️ 已暂停，点击继续恢复', 'warning');
    } else {
        addLog('▶️ 继续填充...', 'success');
    }
}
```

---

## 关键知识点

### 1. SSE vs WebSocket

| 特性 | SSE | WebSocket |
|------|-----|-----------|
| 方向 | 单向（服务器→客户端） | 双向 |
| 协议 | HTTP | HTTP + WebSocket 协议 |
| 复杂度 | 简单 | 较复杂 |
| 适用场景 | 服务端推送实时数据 | 双向实时通信 |

本场景使用 SSE 因为只需服务端单向推送填充指令。

### 2. 异步处理

为什么在 `new Thread()` 中处理？

```java
new Thread(() -> {
    loanFormService.processDocumentAndFill(docId, emitter);
}).start();
```

- AI 调用耗时较长（几秒到几十秒）
- 不阻塞 Servlet 线程
- 保持 SSE 连接持续发送数据

### 3. AI 提取的可靠性

**挑战**：AI 可能返回格式错误或不完整的数据

**解决方案**：
```java
// 清理可能的 markdown 代码块标记
if (response.startsWith("```json")) {
    response = response.substring(7);
}
if (response.startsWith("```")) {
    response = response.substring(3);
}
```

### 4. 前后端数据流

```
前端选择 → 后端读取 → AI 提取 → JSON 解析 → 逐步发送 → 前端渲染
   (docId)   (文档内容)   (结构化)    (对象)      (SSE)      (DOM更新)
```

---

## 扩展方向

1. **支持文件上传**
   - 添加文件上传接口
   - 解析 PDF/Word 文档

2. **增强 AI 理解**
   - 使用更强大的模型
   - 添加少样本示例（Few-shot）

3. **人工审核流程**
   - AI 填充后标记不确定字段
   - 要求人工确认

4. **表单验证**
   - 填充完成后自动校验
   - 提示错误字段

5. **保存草稿**
   - 支持保存填写进度
   - 后续继续填写

---

## 总结

这个 demo 展示了 AI 如何通过以下步骤实现智能表单填充：

1. **Prompt Engineering**: 设计让 AI 输出结构化数据的提示词
2. **流式处理**: 使用 SSE 实现实时反馈
3. **前后端协作**: 后端提取、前端渲染的分工配合
4. **人工介入**: 暂停/继续机制保证可控性

这些技术可以应用到各种表单自动化场景，如：
- 保险理赔申请
- 医院病历录入
- 采购订单填写
- 员工入职登记

**核心思想**: 让 AI 处理信息提取和初步填充，让人类专注于审核和决策。

---

## 来源文件：10-agent-loop-design.md

> [!note] 原文链接
> [[10-agent-loop-design.md]]

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

---

## 来源文件：AI_LEARNING_PATH.md

> [!note] 原文链接
> [[AI_LEARNING_PATH.md]]

# AI Agent 开发学习路线图

## 目标

帮助 Java 开发者系统学习 AI Agent 开发，从使用 AI 工具到开发 AI 应用，掌握 LLM 应用开发的核心技能。

---

## 学习路线

本项目采用递进式学习设计，共 8 个专题，建议按顺序学习：

```
第 1 周：基础入门
┌──────────────────┐  ┌──────────────────┐
│ 01 快速开始       │→│ 02 基础对话       │
│ 项目架构 + 环境   │  │ 流式 + 对话管理   │
└──────────────────┘  └──────────────────┘

第 2 周：核心技能
┌──────────────────┐  ┌──────────────────┐
│ 03 多模型接入     │→│ 04 Prompt 工程    │
│ Profile 切换      │  │ 模板 + 系统提示词  │
└──────────────────┘  └──────────────────┘

第 3 周：Agent 能力
┌──────────────────┐  ┌──────────────────┐
│ 05 Function      │→│ 06 RAG 基础       │
│ Calling          │  │ 向量检索 + 知识库  │
└──────────────────┘  └──────────────────┘

第 4 周：进阶优化
┌──────────────────┐  ┌──────────────────┐
│ 07 RAG 进阶       │→│ 08 成本与可观测性  │
│ 混合检索 + 管道   │  │ AOP + 缓存        │
└──────────────────┘  └──────────────────┘
```

---

## 各周详细计划

### 第 1 周：基础入门（建议 8-10 小时）

#### 01 - 项目快速开始与架构总览
**学习目标**：理解项目全貌，成功运行项目
- 了解 Spring Boot + Spring AI 的技术栈组合
- 理解分层架构：Controller -> Service -> AI Model
- 成功启动项目并通过浏览器访问

**阅读文档**：[01-quick-start.md](01-quick-start.md)
**动手实验**：运行项目，通过 Swagger 测试 API

#### 02 - 基础对话：ChatClient、流式响应与对话管理
**学习目标**：掌握 Spring AI 对话核心机制
- ChatClient vs ChatModel 的区别和使用场景
- SSE 流式响应的实现原理
- 滑动窗口对话历史管理

**阅读文档**：[02-chat-basics.md](02-chat-basics.md)
**动手实验**：
1. 修改系统提示词，观察 AI 行为变化
2. 调整滑动窗口大小，测试多轮对话
3. 用 curl 测试流式接口，观察 SSE 数据格式

---

### 第 2 周：核心技能（建议 8-10 小时）

#### 03 - 多 LLM 接入：Provider 抽象与 Profile 切换
**学习目标**：理解多模型接入的设计模式
- Spring AI 的 Provider 抽象层
- Spring Profile 实现零代码模型切换
- 不同模型的特性和选型策略

**阅读文档**：[03-multi-llm.md](03-multi-llm.md)
**动手实验**：
1. 切换不同模型，对比同一问题的回答
2. 尝试接入一个新的 LLM 提供商

#### 04 - Prompt 工程：模板管理与系统提示词设计
**学习目标**：掌握 Prompt 设计和管理的工程化方法
- System Prompt 的设计原则
- Prompt 模板外部化和热加载
- Few-shot、CoT 等常用技巧

**阅读文档**：[04-prompt-engineering.md](04-prompt-engineering.md)
**动手实验**：
1. 修改系统提示词让 AI 扮演不同角色
2. 添加 Few-shot 示例提升特定场景回答质量

---

### 第 3 周：Agent 能力（建议 10-12 小时）

#### 05 - Function Calling：让 AI 调用后端工具
**学习目标**：理解 AI Agent 的核心机制 -- 工具使用
- Spring AI Function Calling 的自动发现机制
- LLM 如何自主决定调用哪个函数
- 复杂业务逻辑的函数封装

**阅读文档**：[05-function-calling.md](05-function-calling.md)
**动手实验**：
1. 测试贷款查询和风险评估函数
2. 添加一个自定义函数（如客户信息查询）
3. 挑战：实现组合函数，先查数据再给建议

#### 06 - RAG 基础：向量检索与知识库构建
**学习目标**：掌握 RAG 的核心概念和基础实现
- 向量嵌入（Embedding）原理
- 文档加载、分块、存储的完整流程
- Spring AI 的 VectorStore 抽象

**阅读文档**：[06-rag-basics.md](06-rag-basics.md)
**动手实验**：
1. 添加新知识文档，观察检索效果
2. 调整相似度阈值，对比检索精度
3. 调整分块大小，理解 chunk 对检索的影响

---

### 第 4 周：进阶优化（建议 10-12 小时）

#### 07 - RAG 进阶：查询优化、混合检索与智能管道
**学习目标**：掌握生产级 RAG 系统的优化技巧
- 查询重写：指代消解和上下文补全
- 查询扩展：同义词和专业术语扩展
- BM25 + 向量混合检索 + RRF 融合
- LLM 重排序
- 智能策略选择管道
- RAG 效果评估（Recall, Precision, F1, MRR, NDCG）

**阅读文档**：[07-rag-advanced.md](07-rag-advanced.md)
**动手实验**：
1. 对比不同检索策略的结果差异
2. 添加同义词观察召回率变化
3. 使用 evaluate API 量化评估检索效果

#### 08 - 成本管理与可观测性
**学习目标**：掌握 AI 应用的生产级运维能力
- AOP 自动记录 AI 调用日志
- 按 Token 计费的成本计算
- 多级缓存策略（Caffeine）
- 定时统计和成本预警

**阅读文档**：[08-cost-and-observability.md](08-cost-and-observability.md)
**动手实验**：
1. 观察缓存命中率
2. 修改定价表观察成本变化
3. 实现按会话统计成本

---

## 职业发展方向

| 方向 | 职责 | 核心技能 | 需求 |
|------|------|---------|------|
| AI 应用开发 | 基于 LLM 开发应用 | Java + Spring AI + RAG | 高 |
| AI 工程/MLOps | AI 模型部署运维 | Docker + K8s + 监控 | 中高 |
| AI 产品经理 | AI 产品规划 | AI 能力 + 产品设计 | 中 |
| 算法工程师 | 模型训练优化 | Python + PyTorch + 数学 | 门槛高 |

---

## 学习资源

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Prompt Engineering Guide](https://www.promptingguide.ai/zh)
- [DeepLearning.AI 免费课程](https://www.deeplearning.ai/)
- [OpenAI API 文档](https://platform.openai.com/docs)
- [OpenAI API 文档](https://platform.openai.com/docs/overview)

---

## 学习建议

1. **边学边练**：每个文档都有动手实验，务必实际操作
2. **先跑通再深入**：先让项目跑起来，再逐个模块深入理解
3. **对比思考**：切换不同模型、不同参数，观察效果差异
4. **记录笔记**：记录实验结果和思考，形成自己的知识体系
5. **循序渐进**：按周计划推进，不要跳过基础直接看 RAG 进阶

---
