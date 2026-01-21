# DeepSeek 配置完整指南

DeepSeek 是国内优秀的大模型服务商，提供高性价比的 AI API，兼容 OpenAI 格式。

---

## 为什么选择 DeepSeek？

- ✅ **国内服务**：访问速度快，无需翻墙
- ✅ **价格实惠**：比 OpenAI 便宜很多
- ✅ **API 兼容**：完全兼容 OpenAI API 格式
- ✅ **中文友好**：对中文支持优秀
- ✅ **代码能力强**：DeepSeek-Coder 模型在代码任务上表现出色

---

## 快速配置（3 步）

### 第 1 步：获取 DeepSeek API Key

1. 访问 DeepSeek 官网：https://platform.deepseek.com/
2. 注册/登录账号
3. 进入控制台：https://platform.deepseek.com/api_keys
4. 创建 API Key（会得到类似 `sk-xxxxx` 的密钥）
5. **重要**：复制并保存好 API Key，只会显示一次！

**费用说明：**
- 新注册用户通常有免费额度
- 价格远低于 OpenAI（约 1/10 到 1/20）

### 第 2 步：配置项目

编辑 `.env` 文件，填入你的 API Key：

```bash
# DeepSeek API 配置
OPENAI_API_KEY=sk-你的实际deepseek-api-key

# DeepSeek API 地址
OPENAI_BASE_URL=https://api.deepseek.com
```

**注意：**
- `OPENAI_API_KEY` 变量名不变（因为我们使用 OpenAI 兼容格式）
- `OPENAI_BASE_URL` 改为 DeepSeek 的地址

### 第 3 步：在 IDEA 中配置环境变量

1. 打开 Run → Edit Configurations
2. 选择 `AiDevelopApplication`
3. 在 "Environment variables" 中添加：
   ```
   OPENAI_API_KEY=sk-你的实际deepseek-api-key
   OPENAI_BASE_URL=https://api.deepseek.com
   ```
4. 确保 Active profiles 为 `openai`

---

## DeepSeek 模型说明

### 主要模型

| 模型名称 | 适用场景 | 特点 |
|---------|---------|------|
| `deepseek-chat` | 通用对话 | 综合能力强，适合日常对话 |
| `deepseek-coder` | 代码相关 | 代码生成、理解、调试能力强 |

**本项目默认使用：** `deepseek-chat`

**如果你主要做代码相关工作**，可以修改 `application-openai.yml` 中的模型：
```yaml
chat:
  options:
    model: deepseek-coder
```

---

## 启动应用

### 方法 1：在 IDEA 中启动（推荐）

1. 确保已配置环境变量（见第 3 步）
2. 点击运行按钮 ▶️
3. 等待启动完成

### 方法 2：命令行启动

```bash
cd /Users/arvin/java/aidevelop

# 加载环境变量
source .env

# 启动应用
mvn spring-boot:run --settings settings.xml
```

---

## 验证配置

### 1. 查看启动日志

启动成功后，应该看到：
```
2026-01-21 XX:XX:XX [main] INFO  c.e.a.config.AiModelConfig - 初始化 ChatClient，使用提供商: OpenAI
2026-01-21 XX:XX:XX [main] INFO  c.e.a.AiDevelopApplication - Started AiDevelopApplication in X.XXX seconds
```

**注意：** 日志中显示 "OpenAI" 是正常的，因为我们使用的是 OpenAI 兼容模式。

### 2. 测试健康检查

访问：http://localhost:8080/health

应该返回：
```json
{
  "status": "UP",
  "service": "ai-chat-assistant"
}
```

### 3. 测试 Web 界面

访问：http://localhost:8080/index.html

输入消息测试对话功能，例如：
- "你好，介绍一下你自己"
- "用 Java 写一个冒泡排序"
- "解释一下 Spring Boot 的工作原理"

如果能正常回复，说明配置成功！🎉

---

## 使用技巧

### 1. 选择合适的模型

**日常对话、问答：**
```yaml
model: deepseek-chat
```

**代码生成、调试、解释：**
```yaml
model: deepseek-coder
```

### 2. 调整参数优化效果

在 `application-openai.yml` 中：

```yaml
chat:
  options:
    model: deepseek-chat
    temperature: 0.7    # 创造性：0.0-1.0，越高越随机
    max-tokens: 2000    # 最大生成长度
```

**参数建议：**
- **精确任务**（代码、计算）：`temperature: 0.0-0.3`
- **创意任务**（写作、头脑风暴）：`temperature: 0.7-1.0`
- **一般对话**：`temperature: 0.7`（默认）

### 3. 成本控制

DeepSeek 按 token 计费，控制成本的方法：
- 调整 `max-tokens` 限制输出长度
- 修改对话历史保留数量（`application.yml` 中的 `max-history-size`）
- 在控制台查看用量统计

---

## 常见问题

### Q1: API Key 无效

**错误信息：** `401 Unauthorized` 或 `Invalid API Key`

**解决方法：**
1. 检查 API Key 是否正确复制（注意前后空格）
2. 确认 API Key 在 DeepSeek 控制台中是启用状态
3. 检查账户是否有余额
4. 重新生成一个新的 API Key

### Q2: 连接超时

**错误信息：** `Connection timeout` 或 `Read timeout`

**解决方法：**
1. 检查网络连接
2. 确认 `OPENAI_BASE_URL` 为 `https://api.deepseek.com`
3. 尝试增加超时时间（在 `application.yml` 中添加）：
   ```yaml
   spring:
     ai:
       openai:
         chat:
           options:
             timeout: 60000  # 60秒
   ```

### Q3: 模型不存在

**错误信息：** `Model not found` 或 `Invalid model`

**解决方法：**
确认 `application-openai.yml` 中的模型名称为：
- `deepseek-chat` 或
- `deepseek-coder`

不要使用 OpenAI 的模型名称如 `gpt-4`。

### Q4: 中文乱码

**解决方法：**
DeepSeek 默认支持中文，一般不会有问题。如果遇到：
1. 确认 IDE 编码设置为 UTF-8
2. 检查配置文件编码
3. 在系统提示词中明确要求用中文回答

### Q5: 如何切换回 OpenAI 或其他服务？

只需修改 `.env` 文件：

```bash
# 使用 OpenAI
OPENAI_API_KEY=sk-your-openai-key
OPENAI_BASE_URL=https://api.openai.com

# 使用其他服务（如 aicodemirror）
OPENAI_API_KEY=sk-your-other-key
OPENAI_BASE_URL=https://api.other-service.com
```

并修改 `application-openai.yml` 中的模型名称。

---

## API 使用示例

### 1. 普通对话测试

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "你好，介绍一下 DeepSeek"
  }'
```

### 2. 流式对话测试

```bash
curl -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{
    "message": "用 Java 写一个快速排序算法"
  }'
```

### 3. 多轮对话测试

```bash
# 第一轮
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "什么是 Spring Boot？"
  }'

# 记录返回的 conversationId

# 第二轮（使用相同的 conversationId）
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "它有什么优点？",
    "conversationId": "上一步返回的-uuid"
  }'
```

---

## 进阶配置

### 1. 为不同环境配置不同模型

创建 `application-deepseek-coder.yml`：

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.deepseek.com
      chat:
        options:
          model: deepseek-coder
          temperature: 0.3  # 代码任务用较低温度
          max-tokens: 2000
```

启动时指定：
```bash
mvn spring-boot:run -Dspring.profiles.active=deepseek-coder
```

### 2. 配置重试机制

在 `application.yml` 中：
```yaml
spring:
  ai:
    retry:
      max-attempts: 3      # 最多重试3次
      backoff-delay: 1000  # 重试间隔1秒
```

---

## 费用预估

DeepSeek 的价格（截至 2026 年初）：

**deepseek-chat：**
- 输入：约 ￥0.001 / 1K tokens
- 输出：约 ￥0.002 / 1K tokens

**deepseek-coder：**
- 输入：约 ￥0.001 / 1K tokens
- 输出：约 ￥0.002 / 1K tokens

**对比 OpenAI（GPT-4）：**
- OpenAI 约贵 10-20 倍

**使用建议：**
- 学习阶段：DeepSeek 性价比最高
- 生产环境：根据需求选择，DeepSeek 对中文和代码任务很友好

---

## 资源链接

- **DeepSeek 官网**：https://www.deepseek.com/
- **API 平台**：https://platform.deepseek.com/
- **API 文档**：https://platform.deepseek.com/api-docs/
- **定价信息**：https://platform.deepseek.com/api-docs/pricing/
- **使用示例**：https://platform.deepseek.com/api-docs/quick-start/

---

## 技术支持

如果遇到问题：
1. 查看 DeepSeek 官方文档
2. 检查项目日志：`logs/aidevelop.log`
3. 在 DeepSeek 平台提交工单
4. 查看本项目的其他文档

---

## 总结

使用 DeepSeek 的优势：
- ✅ 国内访问快
- ✅ 价格实惠（约 OpenAI 的 1/10）
- ✅ 中文支持好
- ✅ 代码能力强
- ✅ 完全兼容 OpenAI API

配置完成后，你就可以开始你的 AI 应用开发学习之旅了！🚀

**下一步：**
1. 访问 http://localhost:8080/index.html 测试对话
2. 阅读 [AI_LEARNING_PATH.md](AI_LEARNING_PATH.md) 开始系统学习
3. 尝试修改代码，实现自己的功能
