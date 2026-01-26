# 🚀 AI Chat Assistant 学习之旅

## 📍 你现在的位置

✅ **已完成**：
- Spring Boot + Spring AI 项目完整搭建
- 多 LLM 提供商集成（OpenAI、Claude、通义千问）
- 流式对话功能实现
- Web 聊天界面运行
- 对话历史管理

🎯 **下一步**：深入理解系统设计，掌握扩展和优化能力

---

## 📚 四阶段递进式学习规划

### 阶段分布图

```
阶段 1: 理论基础    (2-3 小时)
   ↓
阶段 2: 代码深度分析 (3-4 小时)
   ↓
阶段 3: 实践操作    (2-3 小时)
   ↓
阶段 4: 扩展功能    (5-7 小时)
```

**总学习时间**：12-17 小时，分多个工作日完成

---

# 阶段 1️⃣：理论基础 - 系统架构理解

**目标**：建立整体认知，理解项目为什么这样设计

**预计时间**：2-3 小时

## 1.1 项目架构解析（45 分钟）

### 学习内容

#### 核心概念：为什么需要分层？

在你现有的应用中，数据流向是这样的：

```
HTTP 请求
   ↓
Controller 层    (接收请求、验证参数、返回响应)
   ↓
Service 层       (核心业务逻辑)
   ↓
Repository 层    (数据存储/读取)
   ↓
Database/Cache
   ↓
HTTP 响应
```

**为什么这样分层很重要？**
- **易维护**：每层职责单一，改动不影响其他层
- **易测试**：可以独立测试每一层
- **易扩展**：添加新功能时改动最少
- **易复用**：Service 层可被多个 Controller 使用

#### AI 应用特别需要分层的原因

1. **LLM 提供商变更**：通过 Config 层自动切换，其他层无需改动
2. **对话逻辑复杂**：Service 层集中管理提示词、历史、流式处理
3. **多个消费端**：可能同时有 Web、API、CLI 等多个入口

### 关键文件阅读

**文件 1**: `/README.md` - 项目全景
- 📖 阅读项目介绍和特性列表
- 🎯 理解这个项目要解决的问题

**文件 2**: `/pom.xml` - 依赖分析
- 📖 查看 Spring Boot 版本：`3.2.x`
- 📖 查看 Spring AI 版本：`1.0.0-M4`
- 📖 查看是否有 OpenAI/Claude/通义千问 的 starter
- 💡 为什么用 starter？它自动配置了 ChatModel Bean

**文件 3**: `/src/main/java/com/example/aidevelop/AiDevelopApplication.java` - 启动类
```java
@SpringBootApplication
public class AiDevelopApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiDevelopApplication.class, args);
    }
}
```
- 📖 理解 @SpringBootApplication 做了什么
- 📖 这是应用的入口点

### 实验 1：观察项目结构

在 IDEA 中：
1. 打开项目窗口（左侧 Project 面板）
2. 展开 `src/main/java/com/example/aidevelop/`
3. 观察包结构：
   - `config/` - Spring 配置
   - `controller/` - REST 端点
   - `service/` - 业务逻辑
   - `repository/` - 数据存储
   - `model/` - 数据模型
   - `exception/` - 异常处理

**思考问题**：
- ❓ 为什么 ChatService 是接口而 ChatServiceImpl 是实现？
- ❓ 如果要添加一个新的 API 端点，应该在哪里改代码？

---

## 1.2 Spring AI 核心概念（45 分钟）

### 学习内容

#### ChatModel vs ChatClient 的关系

```
LLM 提供商
  ↓ (OpenAI SDK / Claude SDK / ...)
ChatModel 接口      ← 底层实现（由 starter 提供）
  ↓ (Spring 自动配置)
ChatClient         ← 统一上层接口（我们使用的）
  ↓
你的应用代码
```

**关键差异**：
- **ChatModel**：与特定 LLM 提供商强绑定，调用方式可能不同
- **ChatClient**：抽象层，所有 LLM 提供商用同一套 API

**例子**：
```java
// 直接用 ChatModel（不推荐）
if (model instanceof OpenAiChatModel) {
    // 处理 OpenAI 特定逻辑
} else if (model instanceof AnthropicChatModel) {
    // 处理 Claude 特定逻辑
}

// 用 ChatClient（推荐，统一接口）
chatClient.prompt()
    .user(message)
    .call()
    .content();
```

#### 多 LLM 提供商自动切换机制

你的项目使用了 Spring Profile，这是关键！

```
启动时指定 Profile
       ↓
Spring 加载相应的 application-{profile}.yml
       ↓
自动装配对应的 ChatModel Bean
       ↓
ChatClient 使用该 Model
```

**具体流程**：

启动命令：
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=openai
```

配置文件：`application-openai.yml`
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      model: gpt-4o-mini
```

代码：`AiModelConfig.java`
```java
@Bean
@Profile("openai")
public ChatClient chatClientForOpenAI(
    @Qualifier("openAiChatModel") ChatModel chatModel) {
    return ChatClient.builder(chatModel)
        .defaultSystem("你是一个专业的 AI 助手")
        .build();
}
```

**Spring 的神奇处理**：
1. 检测 `@Profile("openai")`
2. 只有当 profile 是 openai 时才创建这个 Bean
3. 自动注入对应的 ChatModel
4. 其他层无需改动！

### 关键文件阅读

**文件 1**: `/src/main/resources/application.yml` - 主配置
```yaml
spring:
  application:
    name: aidevelop
  profiles:
    active: openai  # 默认使用 OpenAI
```
- 理解 `active: openai` 的含义

**文件 2**: `/src/main/resources/application-openai.yml` - OpenAI 特定配置
**文件 3**: `/src/main/resources/application-anthropic.yml` - Claude 特定配置

- 📖 对比两个文件的差异
- 💡 理解为什么 API Key 不需要硬编码

**文件 4**: `/src/main/java/com/example/aidevelop/config/AiModelConfig.java` - 配置类
```java
@Configuration
public class AiModelConfig {
    @Bean
    @Profile("openai")
    public ChatClient chatClientForOpenAI(...) { ... }

    @Bean
    @Profile("anthropic")
    public ChatClient chatClientForAnthropic(...) { ... }
}
```

- 📖 理解 @Bean、@Profile、@Qualifier 的作用
- 💡 为什么要用 @Qualifier？

### 实验 2：体验 Profile 切换

1. **打开 IDEA 终端**，运行当前配置：
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=openai
   ```
   应用启动成功，在界面对话

2. **停止应用**（Ctrl+C）

3. **修改配置切换到 Claude**：
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=anthropic
   ```
   应用仍能启动，但现在用的是 Claude

4. **对比观察**：
   - 代码无需改动！
   - 只是改了配置，模型就切换了
   - 这就是 Spring 的配置驱动优势

**思考问题**：
- ❓ 如果要添加 OpenAI 新模型 `gpt-4-turbo`，改哪个文件？
- ❓ 为什么说 Spring Profile 是"无代码切换"？

---

## 1.3 对话流程全景（30 分钟）

### 学习内容

#### 一次对话的完整生命周期

```
用户在页面输入 "你好"
  ↓
浏览器发送 HTTP POST /api/chat
  ↓
ChatController.chat() 接收请求
  ↓
ChatService.chat() 处理业务逻辑
  ├─ ①获取或创建 Conversation 对象
  ├─ ②添加用户消息到历史
  ├─ ③构建完整的 Prompt（历史 + 当前问题）
  ├─ ④调用 ChatClient 获取 AI 回复
  ├─ ⑤添加 AI 回复到历史
  └─ ⑥返回响应
  ↓
浏览器接收 JSON 响应
  ↓
JavaScript 更新页面显示
  ↓
用户看到 AI 回复
```

#### 流式对话 vs 普通对话

**普通对话**：
```
用户问 → 等待完整响应 → 返回完整答案 → 显示
(等待时间较长，不适合长文本)
```

**流式对话**：
```
用户问 → 逐字返回响应 → 显示打字机效果
(实时反馈，用户体验好)
```

**技术对比**：

| 特性 | 普通聊天 | 流式聊天 |
|------|---------|---------|
| 响应方式 | 一次性返回 | 逐块返回 |
| 用户等待 | 长 | 短（有即时反馈） |
| 实现复杂度 | 低 | 中（涉及 SSE） |
| 使用场景 | 短回复 | 长文本生成 |
| 当前项目 | /api/chat | /api/chat/stream |

### 关键文件阅读

**文件 1**: `/src/main/java/com/example/aidevelop/model/entity/Conversation.java` - 对话实体
```java
public class Conversation {
    private String conversationId;
    private List<Message> messages;
    private static final int MAX_HISTORY_SIZE = 10;

    public void addMessage(Message message) { ... }
}
```

- 📖 理解 conversationId 的作用（标识唯一对话）
- 📖 理解 messages 列表（对话历史）
- 💡 为什么限制 MAX_HISTORY_SIZE = 10？

**文件 2**: `/src/main/java/com/example/aidevelop/model/entity/Message.java` - 消息实体
```java
public class Message {
    private String id;
    private MessageRole role;    // SYSTEM, USER, ASSISTANT
    private String content;
    private LocalDateTime timestamp;
    private String model;
}
```

- 📖 理解 role 的三个值
- 💡 为什么要记录 timestamp 和 model？

### 实验 3：跟踪一条消息

1. **打开浏览器开发者工具**（F12）
2. **切换到 Network 标签**
3. **在聊天页面输入 "你好"，点击发送**
4. **观察网络请求**：
   - 请求 URL：`http://localhost:8080/api/chat` 或 `/api/chat/stream`
   - 请求方法：POST
   - 请求体：包含你的消息
   - 响应体：包含 conversationId 和 AI 回复

5. **打开 IDEA，查看服务器日志**：
   - 应该看到类似 `Received message: 你好` 的日志
   - 看到 LLM 返回的结果

**思考问题**：
- ❓ 为什么每次请求都要包含 conversationId？
- ❓ 如果丢弃 conversationId，会发生什么？

---

## ✅ 阶段 1 完成标准

完成以下检查，说明你已掌握理论基础：

- [ ] 能解释项目为什么分成 5 层
- [ ] 理解 ChatModel 和 ChatClient 的区别
- [ ] 明白 Spring Profile 如何实现 LLM 切换
- [ ] 知道一次对话的完整流程
- [ ] 能区分普通聊天和流式聊天的差异
- [ ] 能通过开发者工具看到完整的网络请求

**如果还有疑问**，重点阅读这些文件：
1. `pom.xml` - 再看一遍依赖
2. `AiModelConfig.java` - 多看几遍 @Profile 和 @Bean
3. `Conversation.java` - 理解数据结构

---

---

# 阶段 2️⃣：代码深度分析 - 理解关键实现

**目标**：从代码层面理解系统如何工作，能够修改和扩展

**预计时间**：3-4 小时

## 2.1 Service 层深度剖析（1.5 小时）

### 学习内容

这是项目的核心！所有的聊天逻辑都在这里。

#### ChatServiceImpl 的核心方法

**方法 1：`chat()` - 普通聊天**

打开 `/src/main/java/com/example/aidevelop/service/impl/ChatServiceImpl.java`

```java
public ChatResponse chat(ChatRequest request) {
    // ① 获取或创建对话
    Conversation conversation =
        conversationRepository.getOrCreate(request.getConversationId());

    // ② 添加用户消息到历史
    Message userMessage = new Message(
        UUID.randomUUID().toString(),
        MessageRole.USER,
        request.getMessage(),
        LocalDateTime.now(),
        this.getCurrentModel()
    );
    conversation.addMessage(userMessage);

    // ③ 构建包含历史的 Prompt
    String prompt = buildPromptWithHistory(conversation);

    // ④ 调用 ChatClient 获取 AI 响应
    String aiResponse = chatClient.prompt()
        .user(prompt)
        .call()
        .content();

    // ⑤ 添加 AI 响应到历史
    Message assistantMessage = new Message(
        UUID.randomUUID().toString(),
        MessageRole.ASSISTANT,
        aiResponse,
        LocalDateTime.now(),
        this.getCurrentModel()
    );
    conversation.addMessage(assistantMessage);

    // ⑥ 返回响应
    return ChatResponse.builder()
        .conversationId(conversation.getConversationId())
        .message(aiResponse)
        .model(this.getCurrentModel())
        .build();
}
```

**关键点深入理解**：

1️⃣ **为什么需要 getOrCreate？**
```
首次聊天 → conversationId 为空 → 创建新对话
后续聊天 → conversationId 存在 → 获取已有对话
```

2️⃣ **为什么要调用 addMessage 两次？**
```
第一次：保存用户问题 → AI 可以看到用户问了什么
第二次：保存 AI 回答 → 下一轮对话时 AI 能看到自己的回答
```

3️⃣ **buildPromptWithHistory 的作用**
```
原始输入：message = "2+2等于几?"

构建后的 Prompt:
---
你是一个专业的 AI 助手。

User: 你好
Assistant: 你好！我是一个专业的 AI 助手。

User: 2+2等于几?
Assistant:
---

注意：
- 包含了系统提示词
- 包含了所有历史消息
- 为 AI 提供完整的上下文
```

**方法 2：`streamChat()` - 流式聊天**

```java
public Flux<String> streamChat(ChatRequest request) {
    Conversation conversation =
        conversationRepository.getOrCreate(request.getConversationId());

    Message userMessage = new Message(...);
    conversation.addMessage(userMessage);

    String prompt = buildPromptWithHistory(conversation);

    // 关键差异：使用 stream() 返回 Flux 而不是阻塞等待
    return chatClient.prompt()
        .user(prompt)
        .stream()
        .content()
        .doOnComplete(() -> {
            // 流完成后，保存完整的 AI 响应
            String fullResponse = /* 收集所有块 */;
            Message assistantMessage = new Message(...);
            conversation.addMessage(assistantMessage);
        });
}
```

**关键差异**：
- `call()` - 阻塞直到完整响应
- `stream()` - 立即返回 Flux，逐块发送

**方法 3：`buildPromptWithHistory()` - Prompt 构建**

```java
private String buildPromptWithHistory(Conversation conversation) {
    StringBuilder prompt = new StringBuilder();

    // ① 添加系统提示词
    prompt.append("系统提示词: ").append(SYSTEM_PROMPT).append("\n\n");

    // ② 添加对话历史（除了最后的用户消息）
    for (Message msg : conversation.getMessages()) {
        if (msg.getRole() == MessageRole.SYSTEM) continue;

        String roleLabel = msg.getRole() == MessageRole.USER
            ? "User"
            : "Assistant";
        prompt.append(roleLabel).append(": ")
              .append(msg.getContent()).append("\n");
    }

    // ③ 为 AI 预留回答空间
    prompt.append("Assistant: ");

    return prompt.toString();
}
```

### 关键代码实验

#### 实验 4：修改系统提示词

1. **打开** `ChatServiceImpl.java`
2. **查找** `SYSTEM_PROMPT` 常量（通常在类顶部）
3. **找到这一行**：
   ```java
   private static final String SYSTEM_PROMPT = "你是一个专业的 AI 助手...";
   ```
4. **修改为**：
   ```java
   private static final String SYSTEM_PROMPT = "你是一个代码优化专家，所有回答都要用代码示例。";
   ```
5. **重新启动应用**：
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=openai
   ```
6. **在聊天页面测试**：输入 "怎样提高 Java 性能？"
7. **观察结果**：AI 现在会用代码示例来回答

**思考问题**：
- ❓ 修改提示词后，对话历史会受影响吗？
- ❓ 为什么要把提示词提取成常量？

#### 实验 5：调整历史窗口大小

1. **打开** `Conversation.java`
2. **查找** `MAX_HISTORY_SIZE` 常量
3. **修改为** `private static final int MAX_HISTORY_SIZE = 5;`（从 10 改为 5）
4. **重新启动应用**
5. **进行多轮对话**（至少 10 轮）
6. **观察**：每个对话只会保留最近 5 条消息
7. **对比**：往前翻，找不到最早的对话了

**思考问题**：
- ❓ 为什么不能设置太大？（token 成本）
- ❓ 为什么不能设置太小？（丢失上下文）
- ❓ 什么是"滑动窗口"？

#### 实验 6：观察完整的数据流

1. **打开 IDEA 调试器**
2. **在 `ChatServiceImpl.chat()` 方法的第一行设置断点**
3. **在页面输入消息，发送**
4. **调试器停在断点，按 F10 逐步执行**
5. **观察每一步**：
   - 看 `conversation` 对象
   - 看 `userMessage` 被添加
   - 看 `prompt` 构建的过程
   - 看 `chatClient.prompt()` 的结果
   - 看 `assistantMessage` 被添加

**你将看到**：
- 对话对象的实际结构
- 历史消息列表
- 完整的 Prompt 内容
- AI 的实际返回值

---

## 2.2 Repository 和数据存储（30 分钟）

### 学习内容

#### 对话存储的工作原理

打开 `/src/main/java/com/example/aidevelop/repository/ConversationRepository.java`

```java
@Repository
public class ConversationRepository {
    // 使用 ConcurrentHashMap 存储对话
    // Key: conversationId, Value: Conversation 对象
    private final Map<String, Conversation> conversations =
        new ConcurrentHashMap<>();

    public Conversation getOrCreate(String conversationId) {
        // 如果 conversationId 为 null，生成新 ID
        if (conversationId == null) {
            conversationId = UUID.randomUUID().toString();
        }

        // 获取或创建对话
        return conversations.computeIfAbsent(
            conversationId,
            id -> new Conversation(id)
        );
    }

    public void deleteConversation(String conversationId) {
        conversations.remove(conversationId);
    }
}
```

**关键概念**：

1️⃣ **为什么用 ConcurrentHashMap 而不是 HashMap？**
```
多个用户同时聊天 → 多线程并发访问 → 需要线程安全
ConcurrentHashMap 自动处理并发问题
```

2️⃣ **computeIfAbsent 的作用**
```java
conversations.computeIfAbsent(key, id -> new Conversation(id))
```
相当于：
```java
if (!conversations.containsKey(key)) {
    conversations.put(key, new Conversation(key));
}
return conversations.get(key);
```

3️⃣ **为什么用内存存储？**
```
优点：
- 快速（无数据库 I/O）
- 简单（不需要 SQL）
- 适合演示

缺点：
- 重启后数据丢失
- 不适合生产环境
- 单机限制

生产环境应该：
- 使用数据库（MySQL）
- 或缓存（Redis）
```

#### 数据生命周期

```
①创建对话
   ↓
conversationId: abc-123
conversation: { messages: [] }
   ↓
②添加用户消息
   ↓
conversation: { messages: [User: "你好"] }
   ↓
③添加 AI 回复
   ↓
conversation: { messages: [User: "你好", Assistant: "你好！"] }
   ↓
④多轮对话
   ↓
conversation: {
    messages: [
        User: "你好",
        Assistant: "你好！",
        User: "今天天气怎样？",
        Assistant: "我不知道今天的天气..."
    ]
}
   ↓
⑤用户删除对话
   ↓
conversations 中的该条目被移除
```

### 实验 7：观察内存中的数据结构

1. **在 `ConversationRepository.getOrCreate()` 设置断点**
2. **进行多轮对话**
3. **打开调试器中的 Variables 窗口**
4. **展开 `conversations` Map**
5. **看到**：
   - conversationId（如 `abc-123-def-456`）
   - 对应的 Conversation 对象
   - Conversation 中的 messages 列表
   - 每条消息的详细信息

**观察**：
- 现在有多少个 Conversation 对象？
- 每个 Conversation 中有多少条 Message？

---

## 2.3 Controller 和 API 层（45 分钟）

### 学习内容

#### REST API 设计

打开 `/src/main/java/com/example/aidevelop/controller/ChatController.java`

```java
@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "聊天相关 API")
public class ChatController {

    // ① 普通聊天 API
    @PostMapping
    @Operation(summary = "发送消息", description = "发送单条消息并获得完整回复")
    public ResponseEntity<ChatResponse> chat(
        @RequestBody @Valid ChatRequest request) {
        ChatResponse response = chatService.chat(request);
        return ResponseEntity.ok(response);
    }

    // ② 流式聊天 API
    @PostMapping(value = "/stream",
                 produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式聊天", description = "获得实时流式响应")
    public Flux<String> streamChat(
        @RequestBody @Valid ChatRequest request) {
        return chatService.streamChat(request)
            .map(chunk -> "data: " + chunk + "\n\n");
    }

    // ③ 删除对话
    @DeleteMapping("/{conversationId}")
    @Operation(summary = "清空对话", description = "删除指定对话的历史")
    public ResponseEntity<Void> deleteConversation(
        @PathVariable String conversationId) {
        conversationRepository.deleteConversation(conversationId);
        return ResponseEntity.ok().build();
    }
}
```

**关键注解解释**：

| 注解 | 作用 |
|------|------|
| `@RestController` | 返回 JSON，不是模板 |
| `@RequestMapping("/api/chat")` | 所有端点的前缀 |
| `@PostMapping` | POST 请求 |
| `@RequestBody` | 请求体转为 Java 对象 |
| `@Valid` | 校验参数 |
| `@PathVariable` | URL 路径参数 |
| `@Tag` / `@Operation` | 生成 API 文档 |

#### 流式响应的关键实现

```java
@PostMapping(value = "/stream",
             produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> streamChat(@RequestBody ChatRequest request) {
    return chatService.streamChat(request)
        .map(chunk -> "data: " + chunk + "\n\n");  // 关键！
}
```

**SSE（Server-Sent Events）格式**：
```
data: 这是第一块\n\n
data: 这是第二块\n\n
data: 这是第三块\n\n
```

**为什么要加 `"data: "` 和 `"\n\n"`？**
- SSE 的标准格式
- 浏览器看到这个格式才能正确解析
- 如果没有，数据无法被识别

### 实验 8：对比两个 API

1. **打开 IDEA 终端**
2. **测试普通聊天**：
   ```bash
   curl -X POST http://localhost:8080/api/chat \
     -H "Content-Type: application/json" \
     -d '{"message": "你好"}'
   ```
   **结果**：一次性返回完整 JSON

3. **测试流式聊天**：
   ```bash
   curl -X POST http://localhost:8080/api/chat/stream \
     -H "Content-Type: application/json" \
     -d '{"message": "讲一个故事"}'
   ```
   **结果**：逐块返回 SSE 格式数据，看起来像：
   ```
   data: 从
   data: 前
   data: 有
   data: 一
   ...
   ```

**观察**：
- 普通 API 返回后就完成了
- 流式 API 逐块返回，可能持续几秒

#### 异常处理

打开 `/src/main/java/com/example/aidevelop/exception/GlobalExceptionHandler.java`

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ErrorResponse> handleAiServiceException(
        AiServiceException e) {
        return ResponseEntity.status(500)
            .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
        MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
            .getFieldErrors()
            .get(0)
            .getDefaultMessage();
        return ResponseEntity.status(400)
            .body(new ErrorResponse(message));
    }
}
```

**作用**：
- 捕获所有异常
- 统一返回 JSON 格式的错误信息
- 用户看到的都是友好的错误提示

### 实验 9：在 Knife4j 中测试 API

Knife4j 是 Spring Boot 集成的 API 文档和测试工具

1. **打开浏览器** 访问 `http://localhost:8080/doc.html`
2. **看到 Knife4j 界面**，包含所有 API 端点
3. **点击 "POST /api/chat"**
4. **填入测试数据**：
   ```json
   {
     "message": "Spring AI 是什么？",
     "conversationId": null
   }
   ```
5. **点击 "发送"**
6. **查看响应**：返回 conversationId 和 AI 回复
7. **再次请求**，这次在 conversationId 中填入上次得到的 ID
8. **观察**：AI 能记住之前的对话

**这个实验展示了**：
- 如何测试 API
- conversationId 的作用
- 对话历史的保留

---

## ✅ 阶段 2 完成标准

完成以下检查：

- [ ] 能解释 ChatServiceImpl.chat() 的完整流程
- [ ] 理解 buildPromptWithHistory() 如何构建 Prompt
- [ ] 知道为什么用 ConcurrentHashMap
- [ ] 能解释 SSE 的数据格式和作用
- [ ] 通过 Knife4j 成功调用过两个 API
- [ ] 能修改代码参数并观察效果变化
- [ ] 理解异常处理的工作原理

---

---

# 阶段 3️⃣：实践操作 - 动手改进项目

**目标**：通过实际操作加深理解，获得代码修改能力

**预计时间**：2-3 小时

## 3.1 功能扩展（1 小时）

### 实验 10：添加"对话重新开始"功能

**目标**：在 Service 层添加新方法

#### 步骤 1：修改 ChatService 接口

打开 `/src/main/java/com/example/aidevelop/service/ChatService.java`

在接口中添加新方法：
```java
/**
 * 清空当前对话历史，保留对话 ID
 */
void restartConversation(String conversationId);
```

#### 步骤 2：在 ChatServiceImpl 中实现

打开 `/src/main/java/com/example/aidevelop/service/impl/ChatServiceImpl.java`

实现这个方法：
```java
@Override
public void restartConversation(String conversationId) {
    Conversation conversation =
        conversationRepository.getOrCreate(conversationId);
    // 清空所有消息
    conversation.clearMessages();
}
```

#### 步骤 3：在 Conversation 实体中添加清空方法

打开 `/src/main/java/com/example/aidevelop/model/entity/Conversation.java`

添加方法：
```java
public void clearMessages() {
    // 只保留 SYSTEM 消息
    this.messages = this.messages.stream()
        .filter(m -> m.getRole() == MessageRole.SYSTEM)
        .collect(Collectors.toList());
}
```

#### 步骤 4：在 Controller 中添加新的 REST 端点

打开 `/src/main/java/com/example/aidevelop/controller/ChatController.java`

添加端点：
```java
@PostMapping("/{conversationId}/restart")
@Operation(summary = "重新开始对话", description = "清空对话历史但保留对话 ID")
public ResponseEntity<Void> restartConversation(
    @PathVariable String conversationId) {
    chatService.restartConversation(conversationId);
    return ResponseEntity.ok().build();
}
```

#### 步骤 5：测试

1. **重新编译** `mvn clean compile`
2. **重启应用** `mvn spring-boot:run`
3. **打开 Knife4j** http://localhost:8080/doc.html
4. **进行对话**（几轮，保存 conversationId）
5. **调用新 API** `POST /api/chat/{conversationId}/restart`
6. **再次对话**：应该看到 AI 不记得之前的对话了

**验证**：
- ❓ 新对话和重启后的对话行为是否相同？
- ❓ 如果多次调用 restart，会发生什么？

---

### 实验 11：添加"获取对话摘要"功能

**目标**：在 Service 层添加统计功能

#### 步骤 1：创建摘要 DTO

创建新文件 `/src/main/java/com/example/aidevelop/model/dto/ConversationSummary.java`：

```java
@Data
@Builder
public class ConversationSummary {
    private String conversationId;
    private int messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String latestUserMessage;
}
```

#### 步骤 2：在 ChatService 中添加方法

```java
public ConversationSummary getConversationSummary(String conversationId) {
    Conversation conversation =
        conversationRepository.getOrCreate(conversationId);

    List<Message> messages = conversation.getMessages();
    Message latestUserMsg = messages.stream()
        .filter(m -> m.getRole() == MessageRole.USER)
        .reduce((first, second) -> second)  // 取最后一个
        .orElse(null);

    return ConversationSummary.builder()
        .conversationId(conversation.getConversationId())
        .messageCount(messages.size())
        .createdAt(messages.isEmpty() ? null : messages.get(0).getTimestamp())
        .updatedAt(messages.isEmpty() ? null : messages.get(messages.size()-1).getTimestamp())
        .latestUserMessage(latestUserMsg != null ? latestUserMsg.getContent() : null)
        .build();
}
```

#### 步骤 3：在 Controller 中添加新端点

```java
@GetMapping("/{conversationId}/summary")
@Operation(summary = "获取对话摘要", description = "获取对话的统计信息")
public ResponseEntity<ConversationSummary> getConversationSummary(
    @PathVariable String conversationId) {
    ConversationSummary summary = chatService.getConversationSummary(conversationId);
    return ResponseEntity.ok(summary);
}
```

#### 步骤 4：测试

1. 进行几轮对话
2. 调用 `GET /api/chat/{conversationId}/summary`
3. 查看返回的摘要信息

---

## 3.2 性能优化（45 分钟）

### 实验 12：使用 LRU 缓存优化对话查询

**背景**：如果有大量用户同时聊天，每次查询 Conversation 都从 Map 中读取，可能很慢。可以加缓存。

#### 步骤 1：添加 Caffeine 缓存依赖

编辑 `/pom.xml`，在 dependencies 中添加：
```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

#### 步骤 2：在 Repository 中使用缓存

修改 `ConversationRepository.java`：

```java
@Repository
public class ConversationRepository {
    private final Map<String, Conversation> conversations =
        new ConcurrentHashMap<>();

    // 添加缓存
    private final Cache<String, Conversation> cache =
        Caffeine.newBuilder()
            .maximumSize(1000)  // 最多缓存 1000 个对话
            .expireAfterAccess(Duration.ofHours(1))  // 1 小时无访问则删除
            .build();

    public Conversation getOrCreate(String conversationId) {
        if (conversationId == null) {
            conversationId = UUID.randomUUID().toString();
        }

        final String id = conversationId;

        // 先查缓存，如果没有再查主存储
        return cache.get(id, _ ->
            conversations.computeIfAbsent(id, conversationId_ -> new Conversation(id_))
        );
    }
}
```

#### 步骤 3：测试

1. 重新编译和启动
2. 进行几轮对话
3. 观察日志中缓存的 hit/miss 情况

**效果**：
- 频繁访问的对话从缓存返回，速度更快
- 1 小时内未使用的对话自动清除，节省内存

---

### 实验 13：异步处理流式响应

**背景**：流式响应处理需要时间，可以异步处理以提高响应速度

#### 步骤 1：修改 streamChat 方法

在 `ChatServiceImpl.java` 中，修改 `streamChat` 方法：

```java
@Override
public Flux<String> streamChat(ChatRequest request) {
    Conversation conversation =
        conversationRepository.getOrCreate(request.getConversationId());

    Message userMessage = new Message(
        UUID.randomUUID().toString(),
        MessageRole.USER,
        request.getMessage(),
        LocalDateTime.now(),
        this.getCurrentModel()
    );
    conversation.addMessage(userMessage);

    String prompt = buildPromptWithHistory(conversation);

    // 原始 Flux
    Flux<String> contentStream = chatClient.prompt()
        .user(prompt)
        .stream()
        .content();

    // 包装：在流完成后异步保存历史
    return contentStream
        .collectList()  // 收集所有块
        .flatMapMany(chunks -> {
            String fullResponse = String.join("", chunks);

            // 异步保存到历史
            Mono.fromRunnable(() -> {
                Message assistantMessage = new Message(
                    UUID.randomUUID().toString(),
                    MessageRole.ASSISTANT,
                    fullResponse,
                    LocalDateTime.now(),
                    this.getCurrentModel()
                );
                conversation.addMessage(assistantMessage);
            }).subscribeOn(Schedulers.boundedElastic()).subscribe();

            // 返回原始块流
            return Flux.fromIterable(chunks);
        });
}
```

---

## 3.3 监控和日志（30 分钟）

### 实验 14：添加性能监控

#### 步骤 1：创建性能监控切面

创建新文件 `/src/main/java/com/example/aidevelop/config/PerformanceAspect.java`：

```java
@Aspect
@Component
@Slf4j
public class PerformanceAspect {

    @Around("execution(* com.example.aidevelop.service..*(..))")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint)
        throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("Method: {}, Duration: {}ms", methodName, duration);
            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Method: {} failed after {}ms", methodName, duration, e);
            throw e;
        }
    }
}
```

#### 步骤 2：测试

1. 重新编译启动
2. 进行对话
3. 查看控制台日志中的性能信息
4. 观察哪个方法耗时最长

**输出示例**：
```
INFO: Method: chat, Duration: 250ms
INFO: Method: buildPromptWithHistory, Duration: 5ms
INFO: Method: streamChat, Duration: 2450ms
```

---

## ✅ 阶段 3 完成标准

完成以下任务：

- [ ] 成功添加"重新开始对话"功能
- [ ] 成功添加"获取对话摘要"功能
- [ ] 在 Knife4j 中测试新的 API 端点
- [ ] 添加了 Caffeine 缓存
- [ ] 添加了性能监控切面
- [ ] 能在日志中看到性能数据

---

---

# 阶段 4️⃣：扩展功能 - 进阶开发

**目标**：学习高级特性，准备生产应用

**预计时间**：5-7 小时

## 4.1 多租户支持（1.5 小时）

### 学习内容

**背景**：现在每个 conversationId 都是全局的，任何人都能访问。生产环境需要用户隔离。

### 实验 15：添加基于用户的多租户隔离

#### 步骤 1：修改 Conversation 实体

添加用户 ID：
```java
public class Conversation {
    private String conversationId;
    private String userId;  // 新增：用户 ID
    private List<Message> messages;

    public Conversation(String id, String userId) {
        this.conversationId = id;
        this.userId = userId;
        this.messages = new ArrayList<>();
    }
}
```

#### 步骤 2：修改 Repository 支持多租户

```java
@Repository
public class ConversationRepository {
    // 改为二级 Map：userId -> conversationId -> Conversation
    private final Map<String, Map<String, Conversation>> conversations =
        new ConcurrentHashMap<>();

    public Conversation getOrCreate(String userId, String conversationId) {
        // 先获取用户的对话集合
        Map<String, Conversation> userConversations =
            conversations.computeIfAbsent(userId, _ -> new ConcurrentHashMap<>());

        // 再获取或创建对话
        if (conversationId == null) {
            conversationId = UUID.randomUUID().toString();
        }

        final String id = conversationId;
        return userConversations.computeIfAbsent(id,
            _ -> new Conversation(id, userId));
    }

    public void deleteConversation(String userId, String conversationId) {
        Map<String, Conversation> userConversations =
            conversations.get(userId);
        if (userConversations != null) {
            userConversations.remove(conversationId);
        }
    }
}
```

#### 步骤 3：修改 Service 层接收用户 ID

```java
@Service
public class ChatServiceImpl implements ChatService {

    public ChatResponse chat(String userId, ChatRequest request) {
        Conversation conversation =
            conversationRepository.getOrCreate(userId, request.getConversationId());
        // ... 其他逻辑
    }
}
```

#### 步骤 4：修改 Controller 获取用户信息

```java
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
        @RequestHeader String userId,  // 从请求头获取
        @RequestBody @Valid ChatRequest request) {
        ChatResponse response = chatService.chat(userId, request);
        return ResponseEntity.ok(response);
    }
}
```

#### 步骤 5：测试

1. **用户 1 的请求**：
   ```bash
   curl -X POST http://localhost:8080/api/chat \
     -H "Content-Type: application/json" \
     -H "userId: user-1" \
     -d '{"message": "我叫张三"}'
   ```

2. **用户 2 的请求**：
   ```bash
   curl -X POST http://localhost:8080/api/chat \
     -H "Content-Type: application/json" \
     -H "userId: user-2" \
     -d '{"message": "我叫李四"}'
   ```

3. **验证**：
   - 用户 1 再问 "我叫什么？" → AI 记得是张三
   - 用户 2 再问 "我叫什么？" → AI 记得是李四
   - 两个用户的对话完全隔离

---

## 4.2 数据持久化（2 小时）

### 学习内容

**现状**：对话数据存在内存中，应用重启后丢失
**目标**：使用数据库持久化，重启后数据仍在

### 实验 16：集成 Spring Data JPA 和 MySQL

#### 步骤 1：添加依赖

修改 `pom.xml`：
```xml
<!-- JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- MySQL 驱动 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.0.33</version>
</dependency>
```

#### 步骤 2：创建数据库表结构

创建脚本 `/src/main/resources/schema.sql`：
```sql
CREATE TABLE conversation (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
);

CREATE TABLE message (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content LONGTEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    model VARCHAR(100),
    FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE
);
```

#### 步骤 3：创建 JPA Entity

修改 `Conversation.java`，添加 JPA 注解：
```java
@Entity
@Table(name = "conversation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {
    @Id
    private String id;

    @Column(name = "user_id")
    private String userId;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private List<Message> messages;
}
```

#### 步骤 4：创建 Spring Data Repository

创建 `/src/main/java/com/example/aidevelop/repository/ConversationJpaRepository.java`：
```java
@Repository
public interface ConversationJpaRepository
    extends JpaRepository<Conversation, String> {

    List<Conversation> findByUserId(String userId);
}
```

#### 步骤 5：修改 ConversationRepository 使用数据库

```java
@Repository
public class ConversationRepository {

    private final ConversationJpaRepository jpaRepository;
    private final Cache<String, Conversation> cache;

    public Conversation getOrCreate(String userId, String conversationId) {
        if (conversationId == null) {
            conversationId = UUID.randomUUID().toString();
        }

        final String id = conversationId;

        // 先查缓存
        Conversation cached = cache.getIfPresent(id);
        if (cached != null) {
            return cached;
        }

        // 再查数据库
        Conversation conversation = jpaRepository.findById(id)
            .orElseGet(() -> {
                Conversation newConv = new Conversation(id, userId, new ArrayList<>());
                return jpaRepository.save(newConv);
            });

        cache.put(id, conversation);
        return conversation;
    }

    public void saveConversation(Conversation conversation) {
        jpaRepository.save(conversation);
        cache.put(conversation.getId(), conversation);
    }
}
```

#### 步骤 6：修改 application.yml 配置数据库

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/aidevelop?useSSL=false&serverTimezone=UTC
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
```

#### 步骤 7：测试

1. **启动 MySQL 服务**
2. **创建数据库**：`CREATE DATABASE aidevelop;`
3. **重新启动应用**
4. **进行对话**
5. **检查数据库**：
   ```sql
   SELECT * FROM conversation;
   SELECT * FROM message;
   ```
6. **停止应用再启动**
7. **验证**：旧的对话仍在

---

## 4.3 Function Calling - 让 AI 调用工具（2 小时）

### 学习内容

**概念**：让 AI 能够调用你的代码来完成任务

**例子**：用户问 "现在几点？" → AI 调用你的 `getCurrentTime()` 方法 → 返回实际时间

### 实验 17：实现简单的 Function Calling

#### 步骤 1：定义工具方法

创建 `/src/main/java/com/example/aidevelop/tool/ToolService.java`：
```java
@Service
@Slf4j
public class ToolService {

    @Description("获取当前时间")
    public String getCurrentTime() {
        return LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Description("计算两个数的和")
    public int add(
        @Description("第一个数") int a,
        @Description("第二个数") int b) {
        return a + b;
    }

    @Description("查询城市天气")
    public String getWeather(
        @Description("城市名称") String city) {
        // 模拟调用天气 API
        return city + " 今天晴天，温度 25℃";
    }
}
```

#### 步骤 2：配置 Function Calling

在 `ChatServiceImpl.java` 中：
```java
@Service
public class ChatServiceImpl implements ChatService {

    private final ToolService toolService;

    public ChatResponse chat(String userId, ChatRequest request) {
        Conversation conversation =
            conversationRepository.getOrCreate(userId, request.getConversationId());

        // ... 添加用户消息

        String prompt = buildPromptWithHistory(conversation);

        // 启用 Function Calling
        ChatResponse response = chatClient.prompt()
            .user(prompt)
            .functions(toolService.getCurrentTime(),
                      toolService.add(),
                      toolService.getWeather())
            .call()
            .chatResponse();

        // ... 处理响应
        return response;
    }
}
```

#### 步骤 3：测试

1. 在页面问 "现在几点？"
2. AI 会调用 `getCurrentTime()` 方法
3. 返回实际的当前时间

4. 问 "3 加 5 等于几？"
5. AI 会调用 `add(3, 5)` 方法
6. 返回 8

---

## 4.4 RAG 集成 - 连接知识库（1.5 小时）

### 学习内容

**RAG（Retrieval Augmented Generation）**：
- 从外部知识库检索相关信息
- 增强 AI 的回答准确性
- 支持回答 LLM 训练数据之外的问题

### 实验 18：实现简单的文档检索

#### 步骤 1：创建文档存储

创建 `/src/main/java/com/example/aidevelop/service/DocumentService.java`：
```java
@Service
public class DocumentService {

    private final List<Document> documents = new ArrayList<>();

    @PostConstruct
    public void init() {
        // 初始化示例文档
        documents.add(new Document(
            "spring-ai-intro",
            "Spring AI 是什么",
            "Spring AI 是 Spring 团队开发的框架，用于简化 LLM 应用开发。"));

        documents.add(new Document(
            "spring-ai-features",
            "Spring AI 的特性",
            "Spring AI 提供了 ChatClient、VectorStore、RAG 等功能。"));
    }

    public List<Document> search(String query) {
        // 简单的关键字搜索
        return documents.stream()
            .filter(doc -> doc.getTitle().contains(query)
                        || doc.getContent().contains(query))
            .collect(Collectors.toList());
    }

    @Data
    @AllArgsConstructor
    public static class Document {
        private String id;
        private String title;
        private String content;
    }
}
```

#### 步骤 2：修改 Chat 方法支持 RAG

```java
@Service
public class ChatServiceImpl implements ChatService {

    private final DocumentService documentService;

    public ChatResponse chat(String userId, ChatRequest request) {
        // ... 获取对话

        // 搜索相关文档
        List<DocumentService.Document> relevantDocs =
            documentService.search(request.getMessage());

        // 构建增强的 Prompt
        String enhancedPrompt = buildPromptWithRAG(
            buildPromptWithHistory(conversation),
            relevantDocs);

        // 调用 ChatClient
        String response = chatClient.prompt()
            .user(enhancedPrompt)
            .call()
            .content();

        // ... 保存对话
    }

    private String buildPromptWithRAG(String basePrompt,
                                      List<DocumentService.Document> docs) {
        StringBuilder enhanced = new StringBuilder(basePrompt);

        if (!docs.isEmpty()) {
            enhanced.append("\n\n相关知识库文档：\n");
            for (DocumentService.Document doc : docs) {
                enhanced.append("- ").append(doc.getTitle())
                        .append(": ").append(doc.getContent()).append("\n");
            }
        }

        return enhanced.toString();
    }
}
```

#### 步骤 3：测试

1. 问 "Spring AI 有什么特性？"
2. AI 会检索相关文档
3. 基于检索结果给出更准确的回答

---

## ✅ 阶段 4 完成标准

完成以下任务：

- [ ] 实现了多租户用户隔离
- [ ] 集成了 MySQL 数据库
- [ ] 数据能够持久化和恢复
- [ ] 实现了 Function Calling 工具调用
- [ ] 集成了简单的 RAG 文档检索
- [ ] 所有功能都经过测试验证

---

---

# 📋 学习进度追踪表

## 快速参考

| 阶段 | 主题 | 实验数量 | 预计时间 | 状态 |
|------|------|---------|---------|------|
| 1 | 理论基础 | 3 | 2-3h | ⬜ 未开始 |
| 2 | 代码分析 | 6 | 3-4h | ⬜ 未开始 |
| 3 | 实践操作 | 5 | 2-3h | ⬜ 未开始 |
| 4 | 扩展功能 | 4 | 5-7h | ⬜ 未开始 |

## 详细进度（按实验）

**阶段 1️⃣：理论基础**
- [ ] 实验 1：观察项目结构
- [ ] 实验 2：体验 Profile 切换
- [ ] 实验 3：跟踪一条消息

**阶段 2️⃣：代码分析**
- [ ] 实验 4：修改系统提示词
- [ ] 实验 5：调整历史窗口大小
- [ ] 实验 6：观察完整的数据流
- [ ] 实验 7：观察内存中的数据结构
- [ ] 实验 8：对比两个 API
- [ ] 实验 9：在 Knife4j 中测试 API

**阶段 3️⃣：实践操作**
- [ ] 实验 10：添加"重新开始"功能
- [ ] 实验 11：添加"获取摘要"功能
- [ ] 实验 12：LRU 缓存优化
- [ ] 实验 13：异步处理流式响应
- [ ] 实验 14：添加性能监控

**阶段 4️⃣：扩展功能**
- [ ] 实验 15：多租户用户隔离
- [ ] 实验 16：数据库持久化
- [ ] 实验 17：Function Calling
- [ ] 实验 18：RAG 文档检索

---

# 🎯 建议学习节奏

## Week 1
- **Day 1**：完成阶段 1（理论基础 3 小时）
- **Day 2-3**：完成阶段 2 前半部分（代码分析 2-3 小时/天）
- **Day 4-5**：完成阶段 2 后半部分 + 开始阶段 3

## Week 2
- **Day 1-2**：完成阶段 3（实践操作 2-3 小时/天）
- **Day 3-5**：阶段 4 的前两个实验（多租户 + 数据库）

## Week 3
- **Day 1-3**：完成阶段 4 剩余实验（Function Calling + RAG）
- **Day 4-5**：总结和优化

---

# 💡 学习建议

## 通用学习方法

1. **先理论，后代码**
   - 先理解为什么这样设计
   - 再看具体的代码实现

2. **带着问题读代码**
   - 不是被动阅读，而是寻求答案
   - 每读完一个模块，问自己几个问题

3. **实验驱动学习**
   - 不仅要看，更要改
   - 修改代码，观察效果变化

4. **对比学习**
   - 对比不同的实现方式
   - 理解为什么选择当前的方案

## 遇到问题怎么办？

| 问题类型 | 解决方案 |
|---------|--------|
| 编译错误 | 检查依赖版本、导入是否正确 |
| 运行时错误 | 查看日志、设置断点调试 |
| 理解困难 | 回到理论基础、查看官方文档 |
| 功能不工作 | 使用 Knife4j 测试 API、检查数据库 |

## 推荐资源

- Spring Boot 官方文档：https://spring.io/projects/spring-boot
- Spring AI GitHub：https://github.com/spring-projects/spring-ai
- 项目文档：参考 `/docs` 目录下的文档

---

# 📝 总结

通过这个学习之旅，你将：

**理论层面**：
- 理解 Spring Boot 分层架构
- 掌握 Spring AI 框架和多 LLM 集成
- 学习对话系统的设计原理
- 理解流式处理和实时通信

**实践层面**：
- 能够修改和扩展现有代码
- 学会添加新功能
- 掌握性能优化技巧
- 理解生产环境的要求

**职业层面**：
- 具备 AI 应用开发的基础能力
- 能够架构和实现中小型 AI 系统
- 了解业界最佳实践
- 为进一步深造打下基础

---

**开始你的学习之旅吧！从阶段 1 开始，一步步深入。祝你学习愉快！** 🚀

