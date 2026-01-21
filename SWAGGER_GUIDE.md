# Swagger API 文档使用指南

恭喜！Swagger (OpenAPI 3.0) 已成功集成到项目中。

---

## 快速访问

启动应用后，访问以下地址：

- **Swagger UI（推荐）**：http://localhost:8080/swagger-ui.html
- **API 文档 JSON**：http://localhost:8080/v3/api-docs
- **API 文档 YAML**：http://localhost:8080/v3/api-docs.yaml

---

## Swagger UI 功能介绍

### 1. 界面概览

Swagger UI 提供了一个交互式的 API 文档界面，包含：
- 📋 **API 分组**：按功能模块分类（聊天接口、模型管理、系统管理）
- 📝 **接口详情**：请求参数、响应示例、状态码说明
- 🧪 **在线测试**：可以直接在浏览器中调用 API

### 2. 接口分组

| 分组 | 说明 | 包含接口 |
|------|------|---------|
| **聊天接口** | AI 对话相关 | 普通聊天、流式聊天、清空历史 |
| **模型管理** | 模型信息查询 | 获取当前模型、获取所有模型 |
| **系统管理** | 系统状态 | 健康检查 |

---

## 使用 Swagger 测试 API

### 示例 1：测试普通聊天接口

1. 访问 Swagger UI：http://localhost:8080/swagger-ui.html
2. 找到 **"聊天接口"** 分组
3. 展开 **POST /api/chat** 接口
4. 点击 **"Try it out"** 按钮
5. 在 Request body 中输入：
   ```json
   {
     "message": "你好，介绍一下 Spring AI"
   }
   ```
6. 点击 **"Execute"** 按钮
7. 查看响应结果

**预期响应：**
```json
{
  "conversationId": "uuid-here",
  "message": "Spring AI 是...",
  "model": "deepseek-chat",
  "tokensUsed": 256,
  "responseTime": 1234
}
```

### 示例 2：多轮对话测试

**第一轮对话：**
```json
{
  "message": "什么是 Spring Boot？"
}
```

**第二轮对话（使用第一轮返回的 conversationId）：**
```json
{
  "message": "它有什么优点？",
  "conversationId": "uuid-from-first-response"
}
```

AI 会记住上下文，理解"它"指的是 Spring Boot。

### 示例 3：自定义参数

```json
{
  "message": "写一个 Java 冒泡排序",
  "temperature": 0.3,
  "maxTokens": 2000
}
```

**参数说明：**
- `temperature: 0.3` - 较低温度，输出更精确（适合代码生成）
- `maxTokens: 2000` - 允许生成更长的响应

---

## API 接口详解

### 1. POST /api/chat - 普通聊天

**请求参数：**
```json
{
  "message": "必填，用户消息",
  "conversationId": "可选，对话ID（多轮对话）",
  "model": "可选，指定模型",
  "temperature": "可选，0.0-1.0",
  "maxTokens": "可选，最大生成长度"
}
```

**响应：**
```json
{
  "conversationId": "对话ID",
  "message": "AI回复内容",
  "model": "使用的模型",
  "tokensUsed": 256,
  "responseTime": 1234
}
```

**特点：** 阻塞式，等待完整响应后返回。

---

### 2. POST /api/chat/stream - 流式聊天

**请求参数：** 同普通聊天

**响应格式：** Server-Sent Events (SSE)
```
data: 内容
data: 逐字
data: 返回

```

**特点：**
- 实时流式输出，类似打字机效果
- 适合长文本生成
- 在 Swagger UI 中可能显示不完整（推荐使用前端页面或 curl 测试）

**curl 测试命令：**
```bash
curl -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "讲个笑话"}'
```

---

### 3. DELETE /api/chat/{conversationId} - 清空对话

**路径参数：**
- `conversationId` - 要清空的对话 ID

**响应：** 无内容（204 No Content）

---

### 4. GET /api/models/current - 获取当前模型

**响应：**
```json
{
  "provider": "openai",
  "model": null,
  "isActive": true
}
```

---

### 5. GET /api/models - 获取所有模型

**响应：**
```json
[
  {
    "provider": "openai",
    "model": "gpt-4-turbo-preview",
    "isActive": false
  },
  {
    "provider": "anthropic",
    "model": "claude-3-5-sonnet",
    "isActive": false
  }
]
```

---

### 6. GET /health - 健康检查

**响应：**
```json
{
  "status": "UP",
  "service": "ai-chat-assistant"
}
```

---

## Swagger 高级用法

### 1. 导出 API 文档

**导出为 JSON：**
```bash
curl http://localhost:8080/v3/api-docs -o api-docs.json
```

**导出为 YAML：**
```bash
curl http://localhost:8080/v3/api-docs.yaml -o api-docs.yaml
```

### 2. 使用 Postman 导入

1. 打开 Postman
2. Import → Link
3. 输入：`http://localhost:8080/v3/api-docs`
4. 导入后可以在 Postman 中测试所有 API

### 3. 自定义 Swagger 配置

如果需要修改 Swagger 配置，编辑：
`src/main/java/com/example/aidevelop/config/SwaggerConfig.java`

可以修改的内容：
- API 标题和描述
- 联系人信息
- 许可证信息
- 服务器地址

---

## 常见问题

### Q1: Swagger UI 页面打不开

**解决：**
1. 确认应用已启动：http://localhost:8080/health
2. 检查端口是否被占用
3. 清空浏览器缓存重试
4. 尝试访问：http://localhost:8080/swagger-ui/index.html

### Q2: 流式接口在 Swagger 中显示不完整

**原因：** Swagger UI 对 SSE 流式响应支持有限

**解决：**
- 使用前端页面测试：http://localhost:8080/index.html
- 使用 curl 命令测试（见上文）
- 使用 Postman 的 "Send and Download" 功能

### Q3: 如何添加认证（如果需要）

编辑 `SwaggerConfig.java`，添加安全配置：

```java
@Bean
public OpenAPI openAPI() {
    return new OpenAPI()
        .info(...)
        .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
        .components(new Components()
            .addSecuritySchemes("Bearer Authentication",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
}
```

### Q4: 如何隐藏某些接口

在 Controller 方法上添加：
```java
@Hidden  // 从 Swagger 文档中隐藏此接口
@GetMapping("/internal")
public String internalApi() {
    return "Hidden";
}
```

---

## 最佳实践

### 1. 给 API 添加详细注解

**推荐：**
```java
@Operation(
    summary = "简短描述",
    description = "详细说明，包括使用场景、注意事项等"
)
@ApiResponse(responseCode = "200", description = "成功")
@ApiResponse(responseCode = "400", description = "请求参数错误")
```

### 2. 使用 @Schema 描述字段

**推荐：**
```java
@Schema(
    description = "字段说明",
    example = "示例值",
    required = true
)
private String field;
```

### 3. 分组管理 API

使用 `@Tag` 给 Controller 分组：
```java
@Tag(name = "分组名称", description = "分组说明")
@RestController
public class SomeController {
    // ...
}
```

---

## 资源链接

- **Swagger 官网**：https://swagger.io/
- **SpringDoc 文档**：https://springdoc.org/
- **OpenAPI 规范**：https://spec.openapis.org/oas/v3.0.0

---

## 总结

Swagger 已成功集成，现在你可以：
- ✅ 在浏览器中查看完整的 API 文档
- ✅ 直接在线测试所有接口，无需 Postman
- ✅ 导出 API 文档供团队使用
- ✅ 自动生成客户端代码（各种语言）

**立即体验：** http://localhost:8080/swagger-ui.html 🚀
