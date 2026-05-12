# 10 - Chat Memory：会话持久化与流式会话续聊

## 1. 背景

在 AI 对话系统中，Chat Memory 负责“记住上下文”。如果只放内存，会出现两个明显问题：

- 应用重启后历史丢失
- 流式对话首轮拿不到 `conversationId`，后续无法稳定续聊

本项目当前实现目标：

1. 将会话消息持久化到 MySQL（`chat_message` 表）
2. 在 `/api/chat/stream` 中增加 SSE `meta` 事件，显式回传 `conversationId`
3. 保持原有文本流兼容，不破坏旧前端的文本展示

## 2. 整体架构

```
ChatController
   │
   ▼
ChatServiceImpl
   │
   ├─ ConversationRepository (门面)
   │     └─ ChatMessageRepository (JPA)
   │           └─ chat_message (MySQL)
   │
   └─ ChatClient (LLM)
         ├─ 阻塞：返回 ChatResponse(conversationId)
         └─ 流式：SSE 文本 + SSE meta(conversationId)
```

## 3. 数据模型

新增实体 `ChatMessageEntity`，一条记录代表一条消息（append-only）：

- `id`：自增主键
- `messageId`：业务消息 ID（唯一）
- `conversationId`：会话 ID
- `role`：`SYSTEM/USER/ASSISTANT`
- `content`：消息内容
- `model`：模型名称（可空）
- `createdAt`：消息时间

对应建表脚本：`sql/chat_memory.sql`

关键索引：

- `uk_message_id`：防止重复写入
- `idx_conv_created(conversation_id, created_at)`：支持按会话顺序恢复历史

## 4. Repository 迁移策略

保留 `ConversationRepository` 作为上层门面，内部从内存实现切换为 JPA 持久化实现，降低业务层改动范围：

- `save(conversation)`：遍历会话消息，按 `messageId` 去重后追加写入
- `findById(conversationId)`：按时间升序查询消息并聚合回 `Conversation`
- `delete(conversationId)`：按会话删除全部消息
- `findAll()`：基于去重会话 ID 列表恢复会话集合（主要用于调试/管理）

这种方式对 `ChatServiceImpl`、`QueryRewriteService` 保持接口兼容。

## 5. 流式会话续聊修复

### 5.1 后端

`ChatServiceImpl.streamChat()` 在确定会话 ID 后，先发送元事件：

```text
event: meta
data: {"conversationId":"..."}
```

然后继续发送原有文本分片（`data:`），因此旧客户端仍可显示文本。

### 5.2 前端

`chat.js` 增加 SSE 事件块解析：

- 识别 `event: meta`
- 解析 JSON 提取 `conversationId`
- 立即更新 `this.conversationId`

后续流式请求即可稳定携带同一会话 ID，形成真正多轮对话。

## 6. 初始化与运行

首次初始化数据库建议执行：

```bash
mysql -u root -p < sql/demo_tables.sql
mysql -u root -p < sql/ai_cost_tracking.sql
mysql -u root -p < sql/chat_memory.sql
```

启动后验证：

1. 阻塞对话两轮，确认 `conversationId` 一致
2. 流式首轮返回 `meta`，第二轮可续聊
3. 重启应用后同 `conversationId` 仍能取到历史
4. 调用 `DELETE /api/chat/{conversationId}` 后历史被清空

## 7. 当前边界与后续优化

已完成：

- MySQL 持久化会话
- 流式 `conversationId` 回传
- 兼容旧文本流消费

可继续优化：

- 为会话增加 TTL/归档策略，控制长期增长
- 增加分页历史查询接口（当前是按会话全量聚合）
- 引入摘要压缩（超长历史自动总结）
- 增加并发写入下的更严格一致性控制（如会话级锁/版本号）

## 8. 关键代码文件

| 文件 | 说明 |
|------|------|
| `src/main/java/com/example/aidevelop/model/entity/ChatMessageEntity.java` | Chat Message 持久化实体 |
| `src/main/java/com/example/aidevelop/repository/ChatMessageRepository.java` | 会话消息 JPA 仓储 |
| `src/main/java/com/example/aidevelop/repository/ConversationRepository.java` | 会话门面（聚合/回写） |
| `src/main/java/com/example/aidevelop/service/impl/ChatServiceImpl.java` | 阻塞与流式会话主链路、SSE meta 事件 |
| `src/main/resources/static/js/chat.js` | 前端 SSE 事件解析与 conversationId 更新 |
| `sql/chat_memory.sql` | `chat_message` 表结构与索引 |
