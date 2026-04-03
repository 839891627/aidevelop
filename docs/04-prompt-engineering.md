# 04 - Prompt 工程：模板管理与系统提示词设计

## 1. 核心概念

### System Prompt vs User Prompt

- **System Prompt**：定义 AI 的角色、行为规则和约束，在每次对话中保持不变
- **User Prompt**：用户实际输入的问题或指令

系统提示词决定了 AI 回答的质量和方向，是 Prompt Engineering 中最重要的部分。

### Prompt 模板的作用

- **外部化**：提示词从代码中分离，修改不需要重新编译
- **版本化**：可以追踪提示词的变更历史
- **热加载**：运行时修改提示词，无需重启应用

### 常用 Prompt Engineering 技巧

| 技巧 | 说明 | 适用场景 |
|------|------|---------|
| Role-playing | 给 AI 设定专业角色 | 专业领域问答 |
| Few-shot | 提供几个示例 | 格式化输出、特定风格 |
| CoT (Chain of Thought) | 引导逐步推理 | 复杂逻辑问题 |
| 约束条件 | 限定回答范围和格式 | 避免跑题、控制输出 |

## 2. 本项目的 Prompt 架构

```
application.yml (app.prompt.*)
       │
       ▼
PromptProperties (@ConfigurationProperties)
       │
       ▼
PromptService (加载 classpath:prompts/ 下的模板文件)
       │
       ▼
AiModelConfig (将 prompt 注入 ChatClient.builder())
```

### PromptProperties

将 `application.yml` 中的 `app.prompt.*` 配置绑定到 Java 对象：

```yaml
app:
  prompt:
    system-prompt-path: classpath:prompts/system-prompt.txt
    rag-qa-prompt-path: classpath:prompts/rag-qa-prompt.txt
    function-calling-prompt-path: classpath:prompts/function-calling-prompt.txt
```

### PromptService

核心职责：
1. 从 `classpath:prompts/` 加载模板文件
2. 支持运行时热加载（`/api/prompt/reload`）
3. 文件不存在时回退到硬编码默认值

```java
@Service
public class PromptService {
    private String systemPrompt;

    @PostConstruct
    public void loadPrompts() {
        // 从文件加载，失败则用默认值
        this.systemPrompt = loadFromFileOrDefault(
            properties.getSystemPromptPath(),
            "你是一个专业的金融助贷系统客服..."
        );
    }

    public void reload() {
        loadPrompts(); // 重新加载
    }
}
```

## 3. 系统提示词设计实战

### 设计原则

1. **明确角色定位**：告诉 AI "你是谁"
2. **界定能力边界**：说明 AI 能做什么、不能做什么
3. **提供决策框架**：帮助 AI 判断不同场景的应对策略
4. **设定输出格式**：规范回答的结构和风格

### 本项目的系统提示词分析

项目根据用户问题类型设计了 4 种决策策略：

```
场景 1: 纯规则类问题（如"逾期怎么处理"）
  -> 直接从知识库检索回答，不调用函数

场景 2: 纯数据类问题（如"查一下贷款 LN001"）
  -> 调用函数查询数据库，用真实数据回答

场景 3: 混合类问题（如"这笔贷款逾期了怎么办"）
  -> 先查数据，再结合规则知识回答

场景 4: 假设/通用问题（如"贷款利率一般是多少"）
  -> 给出一般性建议，提示以实际为准
```

### 不同模型的 Prompt 差异

- **DeepSeek/智谱AI**：提示词较短，模型能较好理解隐含意图
- **Claude**：提示词更详细，包含显式的决策指令和输出格式要求

这反映了不同模型对指令粒度的需求不同，实际项目中需要针对使用的模型调整。

## 4. Prompt 模板文件

```
src/main/resources/prompts/
├── system-prompt.txt          # 通用系统提示词
├── rag-qa-prompt.txt          # RAG 问答提示词（指导如何使用检索结果）
└── function-calling-prompt.txt # 函数调用引导提示词
```

## 5. Prompt 管理 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/prompt/current` | 查看当前系统提示词 |
| POST | `/api/prompt/reload` | 热加载提示词文件 |

修改 `src/main/resources/prompts/` 下的文件后，调用 reload 接口即可生效，无需重启。

## 6. 动手实验

### 实验 1：修改 AI 角色

编辑 `system-prompt.txt`，将 AI 改为严格的合规审查员：

```
你是一个严格的金融合规审查员。在回答任何问题时，你必须：
1. 先判断问题是否涉及合规风险
2. 对有风险的操作给出明确的警告
3. 引用相关的监管规定
...
```

重启或调用 reload，观察 AI 回答风格的变化。

### 实验 2：添加 Few-shot 示例

在系统提示词中添加示例对话：

```
示例对话：
用户：贷款利率是多少？
助手：关于贷款利率，不同产品的利率范围如下：
- 产品A：年化 5.5%-8.0%
- 产品B：年化 7.2%-12.0%
请注意，实际利率会根据客户信用评估结果确定。请问您想了解哪个产品？
```

观察添加示例后，AI 输出格式是否更规范。

### 实验 3：A/B 测试

准备两版提示词，分别测试 10 个相同问题，对比：
- 回答准确率
- 回答相关性
- 输出格式规范性

## 7. 关键代码文件

| 文件 | 关注点 |
|------|--------|
| `service/prompt/PromptService.java` | 模板加载和热加载逻辑 |
| `config/PromptProperties.java` | 配置绑定 |
| `controller/PromptController.java` | Prompt 管理 API |
| `config/AiModelConfig.java` | prompt 注入 ChatClient |
| `src/main/resources/prompts/` | 模板文件目录 |
| `src/main/resources/application.yml` | `app.prompt.*` 配置节 |
