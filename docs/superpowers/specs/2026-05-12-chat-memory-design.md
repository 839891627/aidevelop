# Chat Memory 持久化 + 摘要设计

## 概述

将现有的内存对话历史管理替换为 Spring AI `ChatMemory` 接口实现，支持 MySQL 持久化和自动摘要。

## 架构

```
ChatClient
  └── MessageChatMemoryAdvisor（Spring AI 内置 Advisor）
        └── SummarizingChatMemory（装饰器，处理摘要逻辑）
              └── JpaChatMemory（持久化层，实现 ChatMemory 接口）
                    └── MySQL（chat_message 表）
```

## 数据模型

### chat_message 表

```sql
CREATE TABLE chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL,        -- USER / ASSISTANT / SYSTEM
    content TEXT NOT NULL,
    is_summary BOOLEAN DEFAULT FALSE, -- 标记是否为摘要消息
    created_at DATETIME NOT NULL,
    INDEX idx_conv_id_created (conversation_id, created_at)
);
```

设计要点：
- `is_summary` 区分普通消息和摘要消息
- 索引 `(conversation_id, created_at)` 支持高效的"取最近 N 条"查询
- 不单独建 conversation 表，对话存在性由消息隐含

### JPA 实体：ChatMessageEntity

字段与表一一映射，使用 `@Entity` + `@Table(name = "chat_message")`。

## 核心组件

### 1. JpaChatMemory implements ChatMemory

纯持久化层，实现三个方法：
- `add(conversationId, messages)` — Message 转 Entity 存入 MySQL
- `get(conversationId, lastN)` — 按 created_at 倒序取最近 N 条，转回 Message
- `clear(conversationId)` — 删除该会话所有消息

类型映射：UserMessage → USER, AssistantMessage → ASSISTANT, SystemMessage → SYSTEM

### 2. SummarizingChatMemory implements ChatMemory（装饰器）

```java
public class SummarizingChatMemory implements ChatMemory {
    private final ChatMemory delegate;       // JpaChatMemory
    private final ChatClient chatClient;     // 生成摘要用
    private final int windowSize;            // 窗口大小（默认 10）
    private final int summarizeThreshold;    // 触发摘要的消息数（默认 20）
}
```

`get()` 逻辑：
1. 从 delegate 取出所有消息（含已有摘要）
2. 如果总消息数 > summarizeThreshold，将窗口外的旧消息用 LLM 压缩为一条摘要
3. 摘要存回（is_summary=true），删除被摘要的原始消息
4. 返回：摘要消息（如有）+ 最近 windowSize 条消息

### 3. ChatMemoryConfig 配置类

```java
@Configuration
public class ChatMemoryConfig {
    @Bean
    public JpaChatMemory jpaChatMemory(ChatMessageRepository repository) { ... }

    @Bean
    public ChatMemory chatMemory(JpaChatMemory jpaChatMemory, ChatClient chatClient,
                                  @Value("${app.chat.memory.window-size:10}") int windowSize,
                                  @Value("${app.chat.memory.summarize-threshold:20}") int threshold) {
        return new SummarizingChatMemory(jpaChatMemory, chatClient, windowSize, threshold);
    }
}
```

## ChatServiceImpl 改造

删除所有手动历史管理代码，改用 Advisor 模式：

```java
var response = chatClient.prompt()
    .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
        .conversationId(conversationId)
        .chatMemoryRetrieveSize(10)
        .build())
    .options(buildRuntimeOptions(request))
    .user(request.getMessage())
    .call()
    .chatResponse();
```

流式 streamChat() 同理，MessageChatMemoryAdvisor 同时支持 aroundCall 和 aroundStream。

## 配置

```yaml
app:
  chat:
    memory:
      window-size: 10
      summarize-threshold: 20
      summary-prompt: classpath:prompts/memory/summarize.txt
```

## 删除清单

- `model/entity/Conversation.java`
- `model/entity/Message.java`
- `model/entity/MessageRole.java`
- `repository/ConversationRepository.java`
- `ChatServiceImpl` 中的 `buildPromptWithHistory()`、`getOrCreateConversation()`、`createNewConversation()`

## 新增文件

- `model/entity/ChatMessageEntity.java` — JPA 实体
- `repository/ChatMessageRepository.java` — Spring Data JPA Repository
- `service/memory/JpaChatMemory.java` — ChatMemory 持久化实现
- `service/memory/SummarizingChatMemory.java` — 摘要装饰器
- `config/ChatMemoryConfig.java` — 配置类
- `resources/prompts/memory/summarize.txt` — 摘要 prompt 模板
- `resources/db/migration/V2__create_chat_message.sql` — 建表 DDL

## 摘要 Prompt 模板

```
请将以下对话历史压缩为简洁的摘要。保留：
- 用户的核心问题和意图
- 重要的结论和决策
- 关键的上下文信息（如用户偏好、约束条件）

丢弃：
- 寒暄和过渡语
- 重复的信息
- 具体的代码细节（除非是核心讨论点）

对话历史：
{messages}

摘要：
```
