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

Spring AI 的核心设计：所有 LLM 提供商都实现相同的 `ChatModel` 接口。无论底层是 OpenAI 兼容模型还是其他提供商，上层代码通过 `ChatClient` 调用时完全一致。

```
ChatController -> ChatService -> ChatClient -> ChatModel（接口）
                                                  |
                                    +-------------+-------------+
                                    |             |             |
                              OpenAiChatModel                 其他 ChatModel 实现
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
| OpenAI Compatible（自定义） | `application-openai.yml` | `openai` | `${OPENAI_CHAT_MODEL}` | - | 对话 |
| Ollama | `application.yml`（ollama 段） | default（始终加载） | - | `nomic-embed-text` | RAG 向量化 |

注意：Ollama Embedding 始终加载，用于 RAG 文档向量化，与对话模型解耦。

---

## 3. 配置文件详解

### application-openai.yml（DeepSeek）

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: ${OPENAI_BASE_URL}
      chat:
        options:
          model: ${OPENAI_CHAT_MODEL}
          temperature: 0.7
          max-tokens: 1000
```

DeepSeek 提供 OpenAI 兼容接口，所以使用 Spring AI 的 `spring-ai-openai-spring-boot-starter`，只需将 `base-url` 指向 DeepSeek 的 API 地址（通过环境变量 `OPENAI_BASE_URL` 配置）。

项目当前使用 OpenAI 兼容 starter，配置前缀为 `spring.ai.openai`。如需切换为其他兼容模型，只需修改 `OPENAI_BASE_URL` 与 `OPENAI_CHAT_MODEL`。

### application.yml 中的 Ollama Embedding 配置

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      embedding:
        enabled: true
        options:
          model: nomic-embed-text
```

Ollama 在本项目中不用于对话，只用于 Embedding（将文档向量化供 RAG 检索使用）。

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

`AiModelConfig` 当前只在 `openai` profile 下装配一个 `ChatClient`，核心能力为：系统提示词、Function Calling、可选 `QuestionAnswerAdvisor`（基础 RAG）。

### OpenAI Profile 的 ChatClient

```java
@Bean
@Profile("openai")
public ChatClient chatClientForOpenAI(
        @Qualifier("openAiChatModel") ChatModel chatModel,
        @Qualifier("vectorStore") ObjectProvider<VectorStore> vectorStoreProvider) {

    log.info("初始化 ChatClient，使用提供商: OpenAI (DeepSeek)");

    SearchRequest searchRequest = SearchRequest.defaults()
            .topK(ragProperties.getTopK())
            .similarityThreshold(ragProperties.getSimilarityThreshold())
            .build();

    ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(promptService.getSystemPrompt())    // Prompt Registry 生效提示词
            .defaultFunctions("loanQueryFunction", "repaymentQueryFunction", "riskAssessmentFunction")
    ;

    if (ragProperties.isEnabled()) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore != null) {
            builder.defaultAdvisors(new QuestionAnswerAdvisor(vectorStore, searchRequest));
        }
    }

    return builder.build();
}
```

`@Qualifier("openAiChatModel")` 显式指定使用 OpenAI 的 ChatModel Bean，避免多个 ChatModel 实现时冲突。

当前 openai profile 统一通过 `promptService.getSystemPrompt()` 从 Prompt Registry 读取生效系统提示词。

### Bean 命名与冲突处理

当多个 LLM starter 同时引入时，Spring 容器中会存在多个 `ChatModel` Bean。Spring AI 使用 `@Qualifier` 注解区分：

- `openAiChatModel` - 由 `spring-ai-openai-spring-boot-starter` 自动创建

只有与当前 Profile 匹配的 `ChatClient` Bean 会被创建，因此不会冲突。

---

## 5. 如何切换模型

### 通过 Maven 命令切换

```bash
# 使用 DeepSeek（默认，等价于不加 -D 参数）
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
初始化 ChatClient，使用提供商: OpenAI (DeepSeek)
```

或通过阻塞式聊天接口查看响应中的 `model` 字段。

### 环境变量配置

每个提供商需要配置对应的 API Key：

```bash
# DeepSeek（通过 OpenAI 兼容接口）
export OPENAI_API_KEY="sk-xxx"
export OPENAI_BASE_URL="https://api.deepseek.com"

# OpenAI Compatible（例如 GLM）
export OPENAI_CHAT_MODEL="glm-4.5-flash"

# Ollama（Embedding 用，本地服务）
export OLLAMA_BASE_URL="http://localhost:11434"
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

### 步骤 3：添加 AiModelConfig 方法（可选）

若接入的是 OpenAI 兼容接口，通常不需要新增 `ChatClient` 方法，只要调整 `OPENAI_BASE_URL` 和 `OPENAI_CHAT_MODEL` 即可。

仅当接入“非 OpenAI 兼容”的专用 starter 时，才需要在 `AiModelConfig` 中新增对应 profile 的 `ChatClient` Bean。

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
| 大批量 Embedding | Ollama nomic-embed-text | 本地部署、隐私友好 |

---

## 8. 关键代码文件

| 文件 | 关注点 |
|---|---|
| `src/main/java/.../config/AiModelConfig.java` | @Profile 条件化 Bean、ChatClient 构建、Advisor 配置 |
| `src/main/java/.../config/VectorStoreConfig.java` | 向量库初始化、Ollama Embedding 模型绑定 |
| `src/main/java/.../config/RagProperties.java` | RAG 参数配置类（similarityThreshold, topK） |
| `src/main/java/.../config/PromptProperties.java` | 提示词文件路径配置 |
| `src/main/java/.../service/prompt/PromptService.java` | 提示词加载服务（从文件读取系统提示词） |
| `src/main/resources/application.yml` | 公共配置 + Ollama Embedding + 默认 Profile |
| `src/main/resources/application-openai.yml` | OpenAI 兼容配置（DeepSeek/GLM 等） |
| `sql/prompt_registry.sql` | Prompt 注册表结构 |
| `pom.xml` | spring-ai-bom 版本管理、各 LLM starter 依赖 |

路径中的 `...` 代表 `com/example/aidevelop`。
