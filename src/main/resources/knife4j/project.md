# AI Chat Assistant 项目说明

## 项目简介

AI Chat Assistant 是一个基于 Spring Boot + Spring AI 的智能对话助手系统，专为 Java 开发者转型 AI 方向设计。

## 核心功能

### 1. 多 LLM 支持
- OpenAI GPT-4
- DeepSeek（推荐国内用户）
- 阿里通义千问

### 2. 对话模式
- **普通聊天**：阻塞式，返回完整响应
- **流式聊天**：Server-Sent Events，实时逐字返回

### 3. 对话历史
- 智能滑动窗口管理
- 自动保留上下文
- 优化 token 使用成本

## 快速开始

### 第一步：调用普通聊天接口

```bash
POST /api/chat
Content-Type: application/json

{
  "message": "你好，介绍一下 Spring AI"
}
```

### 第二步：使用返回的 conversationId 继续对话

```bash
POST /api/chat
Content-Type: application/json

{
  "message": "它有什么优点？",
  "conversationId": "uuid-from-previous-response"
}
```

## 接口说明

### 1. 普通聊天：POST /api/chat
- 发送消息给 AI
- 等待完整响应后返回
- 适合短对话、问答场景

### 2. 流式聊天：POST /api/chat/stream
- 发送消息给 AI
- 实时逐字返回（SSE 流式）
- 适合长文本生成、打字机效果

### 3. 清空历史：DELETE /api/chat/{conversationId}
- 清空指定对话的所有历史消息
- 释放内存

### 4. 获取当前模型：GET /api/models/current
- 查看当前使用的 AI 模型提供商

### 5. 健康检查：GET /health
- 检查服务是否正常运行

## 参数说明

### message（必填）
用户发送的消息内容

### conversationId（可选）
对话 ID，用于多轮对话：
- 首次对话不传
- 后续对话传入上次响应中的 conversationId

### temperature（可选）
温度参数，控制输出随机性：
- 范围：0.0 - 1.0
- 较低值（0.0-0.3）：更精确，适合代码、计算
- 中等值（0.7）：平衡，适合日常对话
- 较高值（0.8-1.0）：更有创意，适合写作

### maxTokens（可选）
最大生成 token 数：
- 控制响应长度
- 影响成本

## 使用示例

### 示例 1：简单问答
```json
{
  "message": "什么是 Spring Boot？"
}
```

### 示例 2：代码生成
```json
{
  "message": "用 Java 写一个快速排序算法",
  "temperature": 0.3,
  "maxTokens": 2000
}
```

### 示例 3：创意写作
```json
{
  "message": "写一个科幻小说的开头",
  "temperature": 0.9,
  "maxTokens": 3000
}
```

## 注意事项

1. **API Key 配置**：确保已配置正确的 LLM API Key
2. **成本控制**：注意 maxTokens 和历史消息数量
3. **流式接口**：需要支持 SSE 的客户端
4. **多轮对话**：记得传递 conversationId

## 技术支持

- GitHub: https://github.com/your-username/aidevelop
- 文档: 查看项目 README.md
- 问题反馈: GitHub Issues

---

**祝你使用愉快！🚀**
