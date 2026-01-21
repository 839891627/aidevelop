# AI Chat Assistant - 智能对话助手系统

🤖 **Java 开发者转型 AI 的实战项目** | 基于 Spring Boot + Spring AI 构建

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0--M4-blue.svg)](https://docs.spring.io/spring-ai/)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 项目简介

这是一个完整的 **AI 智能对话助手系统**，专为想要转型 AI 方向的 Java 开发者设计。项目不仅提供了可运行的代码，还包含详细的学习路径规划和技术文档。

### 主要特性

- ✅ **多 LLM 支持**：OpenAI GPT-4、Anthropic Claude、阿里通义千问
- ✅ **流式响应**：Server-Sent Events (SSE) 实时打字机效果
- ✅ **对话历史**：智能滑动窗口管理，优化成本
- ✅ **Web 界面**：简洁美观的聊天界面
- ✅ **RESTful API**：完整的后端 API 接口
- ✅ **学习文档**：从入门到进阶的完整学习路径

### 技术栈

- **后端**: Spring Boot 3.2, Spring AI 1.0.0-M4, Java 17
- **前端**: HTML5, CSS3, JavaScript (原生)
- **构建工具**: Maven 3.9+
- **AI 模型**: OpenAI GPT-4 / Anthropic Claude / 阿里通义千问

---

## 快速开始

### 前提条件

- Java 17 或更高版本
- Maven 3.6 或更高版本
- OpenAI API Key（或其他 LLM 提供商的 API Key）

### 3 步启动

#### 1. 克隆项目（如果使用 Git）

```bash
# 如果你的项目在 Git 仓库中
git clone <your-repo-url>
cd aidevelop
```

#### 2. 配置 API Key

```bash
# 方式 1: 设置环境变量（推荐）
export OPENAI_API_KEY=sk-your-openai-key-here

# 方式 2: 使用其他 LLM
export ANTHROPIC_API_KEY=sk-ant-your-key-here
```

或者创建 `.env` 文件（参考 `.env.example`）

#### 3. 启动应用

```bash
# 使用 Maven 启动
mvn spring-boot:run

# 或指定使用 Claude
mvn spring-boot:run -Dspring-boot.run.profiles=anthropic
```

#### 4. 访问应用

- **Web 界面**: http://localhost:8080/index.html
- **API 文档**: http://localhost:8080/health (健康检查)

---

## 项目结构

```
aidevelop/
├── src/main/java/com/example/aidevelop/
│   ├── AiDevelopApplication.java          # 启动类
│   ├── config/                            # 配置层
│   │   ├── AiModelConfig.java            # AI 模型配置
│   │   └── CorsConfig.java               # 跨域配置
│   ├── model/                            # 数据模型
│   │   ├── dto/                          # 数据传输对象
│   │   └── entity/                       # 实体类
│   ├── repository/                       # 数据访问层
│   ├── service/                          # 业务逻辑层
│   │   └── impl/ChatServiceImpl.java     # 核心业务实现
│   ├── controller/                       # 控制器层
│   │   └── ChatController.java           # 聊天 API
│   └── exception/                        # 异常处理
├── src/main/resources/
│   ├── application.yml                   # 主配置
│   ├── application-openai.yml            # OpenAI 配置
│   ├── application-anthropic.yml         # Claude 配置
│   └── static/                           # 静态资源
│       ├── index.html                    # 聊天界面
│       ├── css/chat.css                  # 样式
│       └── js/chat.js                    # 前端逻辑
├── docs/                                 # 文档目录
│   ├── AI_LEARNING_PATH.md              # 学习路径规划
│   ├── QUICK_START.md                   # 快速开始指南
│   └── API_REFERENCE.md                 # API 文档
├── pom.xml                              # Maven 配置
└── README.md                            # 项目说明
```

---

## 核心功能演示

### 1. 普通聊天

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "介绍一下 Spring AI"
  }'
```

**响应示例**:
```json
{
  "conversationId": "uuid-here",
  "message": "Spring AI 是 Spring 官方推出的...",
  "model": "gpt-4-turbo-preview",
  "tokensUsed": 256,
  "responseTime": 1234
}
```

### 2. 流式聊天

```bash
curl -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{
    "message": "讲个笑话"
  }'
```

**响应**：逐字返回 SSE 格式数据流

### 3. Web 界面

访问 http://localhost:8080/index.html，在浏览器中直接对话。

---

## API 文档

### 聊天接口

#### POST /api/chat
普通聊天（阻塞式返回完整响应）

**请求体**:
```json
{
  "message": "你的问题",
  "conversationId": "可选-对话ID",
  "temperature": 0.7,
  "maxTokens": 1000
}
```

**响应**:
```json
{
  "conversationId": "uuid",
  "message": "AI 的回答",
  "model": "模型名称",
  "tokensUsed": 256,
  "responseTime": 1234
}
```

#### POST /api/chat/stream
流式聊天（SSE 逐字返回）

**请求体**: 同上

**响应**: `text/event-stream` 格式

#### DELETE /api/chat/{conversationId}
清空对话历史

---

## 学习路径

这个项目不仅是一个可运行的应用，更是一个完整的学习项目。

### 📚 学习文档

1. **[AI 学习路径规划](docs/AI_LEARNING_PATH.md)** - 完整的 4 周学习计划
2. **[快速开始指南](docs/QUICK_START.md)** - 详细的环境搭建和启动步骤
3. **[API 参考文档](docs/API_REFERENCE.md)** - 所有 API 接口说明
4. **[进阶功能指南](docs/ADVANCED_FEATURES.md)** - Function Calling、RAG 等高级特性

### 🎯 学习计划

- **第 1 周**: AI 应用开发基础 + Spring AI 入门
- **第 2 周**: 流式响应 + 对话管理 + Prompt Engineering
- **第 3 周**: 多 LLM 集成 + 生产环境优化
- **第 4 周**: Function Calling + RAG + 多模态

详见 [AI_LEARNING_PATH.md](docs/AI_LEARNING_PATH.md)

---

## 技术亮点

### 1. 统一抽象层
通过 Spring AI 实现对多个 LLM 提供商的统一抽象，切换成本极低：

```java
// 统一的 ChatClient 接口
ChatClient.builder(chatModel)
    .defaultSystem("系统提示词")
    .build();
```

### 2. 流式响应
使用 WebFlux 实现真正的流式响应（SSE）：

```java
@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> streamChat(@RequestBody ChatRequest request) {
    return chatService.streamChat(request)
        .map(chunk -> "data: " + chunk + "\n\n");
}
```

### 3. 智能历史管理
滑动窗口机制，自动管理对话历史，优化成本：

```java
public void addMessage(Message message) {
    messages.add(message);
    // 保持历史在限定范围内，保留 SYSTEM 消息
    while (messages.stream()
        .filter(m -> m.getRole() != MessageRole.SYSTEM)
        .count() > maxHistorySize) {
        // 删除最旧的消息
    }
}
```

### 4. 零代码模型切换
通过 Spring Profile 轻松切换 LLM：

```bash
# 使用 OpenAI
mvn spring-boot:run

# 使用 Claude
mvn spring-boot:run -Dspring-boot.run.profiles=anthropic
```

---

## 进阶扩展

项目提供了清晰的扩展路径：

### 短期扩展（1-2 周）
- [ ] Prompt 模板管理
- [ ] Token 计数和成本统计
- [ ] 用户认证和多用户支持

### 中期扩展（1-2 月）
- [ ] **Function Calling** - 让 AI 调用外部工具
- [ ] **RAG 集成** - 知识库问答
- [ ] **数据持久化** - MySQL/Redis
- [ ] **Docker 容器化**

### 长期扩展（3-6 月）
- [ ] **AI Agent 系统** - 自主决策的智能代理
- [ ] **多模态支持** - 图片理解、语音输入
- [ ] **企业级功能** - 多租户、审计、权限管理
- [ ] **Kubernetes 部署** - 生产环境高可用

---

## 常见问题

### Q: 为什么选择 Spring AI 而不是 LangChain4j？

**A**: Spring AI 是 Spring 官方框架，与 Spring 生态集成最好，文档齐全，适合 Spring 开发者。LangChain4j 功能更丰富，但学习曲线稍陡。

### Q: 没有 OpenAI API Key 怎么办？

**A**: 可以使用国内的 LLM：
- 阿里通义千问（便宜，中文好）
- 腾讯混元
- 百度文心一言

或使用 API 代理服务。

### Q: 项目可以商用吗？

**A**: 可以。项目代码使用 MIT 协议，但注意：
- LLM API 调用需要付费
- 商用需要遵守各 LLM 提供商的服务条款

### Q: 适合新手吗？

**A**: 适合有 Java 和 Spring Boot 基础的开发者。如果是 Java 新手，建议先学习 Spring Boot 基础。

---

## 贡献指南

欢迎贡献代码、文档或提出建议！

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

---

## 致谢

- [Spring AI](https://spring.io/projects/spring-ai) - 优秀的 AI 应用开发框架
- [OpenAI](https://openai.com/) - GPT 系列模型
- [Anthropic](https://www.anthropic.com/) - Claude 系列模型
- [阿里云灵积](https://dashscope.aliyun.com/) - 通义千问

---

## 联系方式

- 📧 Email: your-email@example.com
- 💬 Issue: [GitHub Issues](https://github.com/your-username/aidevelop/issues)
- 📖 博客: your-blog-url

---

## 更新日志

### v1.0.0 (2026-01-21)
- ✨ 初始版本发布
- ✅ 支持 OpenAI 和 Anthropic Claude
- ✅ 流式响应功能
- ✅ Web 聊天界面
- ✅ 完整学习文档

---

**⭐ 如果这个项目对你有帮助，请给一个 Star！**

**🚀 开始你的 AI 转型之旅吧！**
