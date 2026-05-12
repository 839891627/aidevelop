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
        @Qualifier("vectorStore") VectorStore vectorStore) {

    // RAG 检索参数（从配置文件读取）
    SearchRequest searchRequest = SearchRequest.defaults()
            .topK(ragProperties.getTopK())           // 返回 Top 5 文档
            .similarityThreshold(ragProperties.getSimilarityThreshold())  // 相似度阈值 0.2
            .build();

    return ChatClient.builder(chatModel)
            .defaultSystem(promptService.getSystemPrompt())  // 从文件加载系统提示词
            .defaultAdvisors(
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
| `QuestionAnswerAdvisor` | RAG 检索增强 | 将用户问题与向量库匹配，把相关文档片段注入到上下文 |

多轮对话记忆由业务层的 `ConversationRepository`（`ConcurrentHashMap`）维护。`defaultFunctions` 注册了三个 Function Calling 函数，LLM 会在需要时自动调用它们查询数据库。系统提示词由 `PromptService` 从 Prompt Registry 的 `ACTIVE` 版本读取。

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

`ChatServiceImpl.streamChat()` 方法返回 `SseEmitter`，实现逐字输出。

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
public SseEmitter streamChat(@Valid @RequestBody ChatRequest request) {
    return chatService.streamChat(request);
}
```

`MediaType.TEXT_EVENT_STREAM_VALUE` 告诉浏览器这是 SSE 响应。服务端使用 `SseEmitter` 持续发送 `data` 事件给客户端。

### Chat 链路与高级 RAG 链路

- `ChatController` 的 `/api/chat` 是主对话链路，可按配置启用内置 `QuestionAnswerAdvisor`（基础 RAG）。
- 高级 RAG（混合检索、重排、评估）由 `RagController` 的 `/api/rag/*` 接口提供。
- 这样可以将“对话体验”与“检索策略实验”分离，降低学习和维护成本。

### 流式 vs 阻塞式对比

| | 阻塞式 (`/api/chat`) | 流式 (`/api/chat/stream`) |
|---|---|---|
| 返回类型 | `ChatResponse`（完整 JSON） | `SseEmitter`（SSE 流） |
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

### ConversationRepository - MySQL 持久化存储

`ConversationRepository` 当前通过 `chat_message` 表持久化每条消息：`findById` 按 `conversation_id + created_at` 聚合恢复对话，`save` 采用消息追加写入，`delete` 按会话清理。

这意味着应用重启后历史仍可恢复，不再是“仅内存态”会话。

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

### 实验 1：切换 System Prompt 版本，观察 AI 行为变化

在 Prompt Registry 中创建并发布新的 `system.default` 版本。例如：

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
| `sql/prompt_registry.sql` | Prompt 注册表结构 |
| `src/main/resources/static/index.html` | 聊天界面 HTML |
| `src/main/resources/static/js/chat.js` | ChatApp 类、流式读取、Markdown 渲染 |
| `src/main/resources/static/css/chat.css` | 界面样式 |

路径中的 `...` 代表 `com/example/aidevelop`。
