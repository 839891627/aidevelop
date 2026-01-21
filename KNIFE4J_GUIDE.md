# Knife4j API 文档使用指南

🎨 **Knife4j** 是国内开发的 Swagger UI 增强版，界面更美观、功能更强大、中文支持更好。

---

## 快速访问

启动应用后，访问以下地址：

### 🔥 推荐使用
- **Knife4j UI**：http://localhost:8080/doc.html

### 其他方式
- **标准 Swagger UI**：http://localhost:8080/swagger-ui.html
- **API 文档 JSON**：http://localhost:8080/v3/api-docs
- **API 文档 YAML**：http://localhost:8080/v3/api-docs.yaml

---

## Knife4j UI 功能介绍

### 1. 界面特色

| 特性 | 说明 |
|------|------|
| **美观设计** | 左右布局，更宽敞的显示空间 |
| **中文支持** | 完全中文界面 |
| **快速搜索** | 方便快速查找接口 |
| **在线调试** | 功能更丰富的接口测试 |
| **代码生成** | 自动生成各语言客户端代码 |
| **接口分享** | 可以分享单个接口链接 |
| **文档下载** | 支持导出为 Word、HTML 等格式 |

### 2. 界面布局

```
┌─────────────────────────────────────────────────────┐
│ 菜单栏                                               │
├──────────────┬──────────────────────────────────────┤
│              │                                       │
│ 左侧菜单     │     中央内容区                        │
│              │                                       │
│ • 聊天接口   │ 接口详情、参数说明、在线测试        │
│ • 模型管理   │                                       │
│ • 系统管理   │                                       │
│              │                                       │
└──────────────┴──────────────────────────────────────┘
```

---

## 使用步骤

### 第一步：访问 Knife4j

1. 启动应用
2. 在浏览器中打开：**http://localhost:8080/doc.html**

### 第二步：选择接口

1. 左侧菜单会显示所有接口分组
2. 点击分组展开接口列表
3. 点击接口进行详细查看

### 第三步：测试接口

1. 点击接口进入详情页
2. 向下滚动找到 **"调试"** 标签
3. 或直接点击接口卡片上的 **"调试"** 按钮
4. 输入请求参数
5. 点击 **"发送"** 按钮
6. 查看响应结果

---

## 实战示例

### 示例 1：测试普通聊天

**步骤：**
1. 访问 http://localhost:8080/doc.html
2. 左侧菜单找到 **"聊天接口"** → **"POST /api/chat"**
3. 点击进入接口详情
4. 点击 **"调试"** 标签
5. 在 Request Body 中输入：
   ```json
   {
     "message": "你好，请介绍一下自己"
   }
   ```
6. 点击 **"发送"** 按钮

**预期响应：**
```json
{
  "conversationId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "你好！我是一个...",
  "model": "deepseek-chat",
  "tokensUsed": 256,
  "responseTime": 1234
}
```

### 示例 2：多轮对话

**第一次请求：**
```json
{
  "message": "什么是 Spring AI？"
}
```

记录返回的 `conversationId`。

**第二次请求（多轮对话）：**
```json
{
  "message": "它支持哪些 LLM？",
  "conversationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

AI 会理解上下文，知道"它"指的是 Spring AI。

### 示例 3：流式聊天测试

**请求：**
```json
{
  "message": "写一个 Spring Boot 应用的基本框架"
}
```

**响应：** 实时逐字返回，模拟打字机效果。

在 Knife4j 中也能看到完整的流式响应。

---

## Knife4j 高级功能

### 1. 快速搜索

**位置：** 左上角搜索框

**用途：**
- 快速查找接口
- 输入接口名称、路径、参数名等
- 支持模糊搜索

### 2. 在线文档

**位置：** 上方菜单栏 "文档" 按钮

**功能：**
- 查看项目文档
- 浏览使用指南
- 阅读技术说明

### 3. 导出文档

**位置：** 右上角下拉菜单

**支持格式：**
- Word（.docx）
- HTML
- Markdown
- OpenAPI JSON

**用途：**
- 生成离线文档
- 分享给团队成员
- 集成到知识库

### 4. 代码生成

**位置：** 接口调试页面右侧

**支持语言：**
- Java
- Python
- JavaScript
- Go
- Rust
- 等多种语言

**用途：**
- 快速生成客户端代码
- 减少手工编码

### 5. 在线分享

**位置：** 接口详情页右上角

**功能：**
- 复制接口链接
- 分享给他人
- 便于协作

---

## 与标准 Swagger 的对比

| 功能 | Swagger UI | Knife4j |
|------|-----------|---------|
| **基础功能** | ✅ | ✅ |
| **美观度** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **中文支持** | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| **快速搜索** | ⭐⭐ | ⭐⭐⭐⭐ |
| **文档导出** | ❌ | ✅ |
| **代码生成** | ❌ | ✅ |
| **接口分享** | ❌ | ✅ |
| **个性化配置** | ⭐⭐ | ⭐⭐⭐⭐ |

---

## 常见问题

### Q1: Knife4j 访问地址是什么？

**A:** http://localhost:8080/doc.html

### Q2: 如何导出 API 文档？

**A:**
1. 访问 Knife4j: http://localhost:8080/doc.html
2. 右上角菜单 → "文档下载"
3. 选择格式（Word、HTML 等）
4. 下载

### Q3: 流式接口在 Knife4j 中如何测试？

**A:**
- 直接在 Knife4j 中测试，支持 SSE 流式显示
- 或使用 curl 命令：
  ```bash
  curl -X POST http://localhost:8080/api/chat/stream \
    -H "Content-Type: application/json" \
    -d '{"message": "讲个笑话"}'
  ```

### Q4: 如何自定义菜单项？

**A:** 修改 `application.yml` 中的 Knife4j 配置：
```yaml
knife4j:
  documents:
    - name: 自定义文档
      locations: classpath:knife4j/custom.md
```

### Q5: 如何隐藏某些接口？

**A:** 在 Controller 方法上添加：
```java
@Hidden
@GetMapping("/internal")
public String internalApi() {
    return "Hidden";
}
```

---

## 最佳实践

### 1. 完善 API 注解

**推荐做法：**
```java
@Operation(
    summary = "发送聊天消息",
    description = "发送消息给 AI，支持多轮对话。" +
                  "首次对话不传 conversationId，" +
                  "后续传入上次响应中的 ID 进行多轮对话。",
    tags = {"聊天接口"}
)
@ApiResponse(responseCode = "200", description = "请求成功")
@ApiResponse(responseCode = "400", description = "参数错误")
@PostMapping
public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
    // ...
}
```

### 2. 提供丰富的示例

**推荐做法：**
```java
@Schema(
    description = "用户消息",
    example = "你好，请介绍一下 Spring AI",
    required = true
)
private String message;
```

### 3. 分类管理接口

**推荐做法：**
```java
@Tag(name = "聊天接口", description = "AI 对话相关的 API")
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    // ...
}
```

### 4. 定期导出文档

**建议：**
- 定期导出 API 文档
- 版本控制
- 分享给团队

---

## 技术支持

### 官方资源

- **Knife4j 官网**：https://doc.xiaominfo.com/
- **GitHub**：https://github.com/xiaoymin/knife4j
- **使用文档**：https://doc.xiaominfo.com/guide/

### 常见问题

- **GitHub Issues**：https://github.com/xiaoymin/knife4j/issues
- **讨论交流**：GitHub Discussions

---

## 总结

Knife4j 提供了比标准 Swagger UI 更好的体验：
- ✅ 更美观的界面设计
- ✅ 完整的中文支持
- ✅ 强大的文档管理功能
- ✅ 便捷的代码生成
- ✅ 灵活的导出选项

**推荐用 Knife4j 替代标准 Swagger UI 进行 API 开发和测试！**

---

## 快速链接

- 🔥 **Knife4j UI**：http://localhost:8080/doc.html
- 📋 **项目文档**：http://localhost:8080/doc.html#/documents
- 🏥 **健康检查**：http://localhost:8080/health
- 💬 **聊天页面**：http://localhost:8080/index.html

**现在就开始使用 Knife4j 吧！🚀**
