# AI 表单自动填充 Demo

> 通过这个实战 demo，理解 AI 如何根据文档自动填充多标签页表单，并实现人工介入打断

---

## 📋 目录

1. [功能介绍](#功能介绍)
2. [核心概念](#核心概念)
3. [技术架构](#技术架构)
4. [代码实现](#代码实现)
5. [AI Prompt 工程](#ai-prompt-工程)
6. [运行体验](#运行体验)
7. [关键知识点](#关键知识点)

---

## 功能介绍

### 业务场景

用户需要填写一份复杂的贷款申请表单，表单分为 3 个标签页：
- **基本信息**：姓名、身份证、电话、地址、职业、收入
- **贷款信息**：贷款类型、金额、期限、用途、还款来源
- **担保信息**：担保方式、抵押物描述、估值、保证人信息

传统的做法是用户逐个手动填写，费时费力且容易出错。

### AI 解决方案

用户只需提供一份自然语言描述的贷款需求文档，AI 即可：
1. 理解文档内容
2. 提取结构化信息
3. 自动填充到对应表单字段
4. 流式展示填充过程
5. 支持人工暂停介入修改

### 演示效果

```
用户操作：
1. 选择"张三个人贷款申请"文档
2. 点击"开始 AI 填充"

AI 处理过程（可视化显示）：
> 正在读取文档内容...
> ✅ 文档读取成功，共 328 字符
> 🔍 正在使用 AI 智能提取信息...
> 📤 向 AI 发送请求...
> 📥 收到 AI 响应，正在解析...
> ✅ AI 已完成信息提取，开始填充表单...
> 📑 切换到标签页：基本信息
> ✏️ 填充字段 [申请人姓名]: 张三
> ✏️ 填充字段 [身份证号]: 110101199001011234
> ...（逐步填充所有字段）
> 🎉 所有字段填充完成！
```

---

## 核心概念

### 1. SSE (Server-Sent Events)

服务端推送技术，允许服务器持续向客户端发送数据流。

```javascript
// 前端建立 SSE 连接
const eventSource = new EventSource('/api/loan-form/fill-stream');

eventSource.onmessage = (event) => {
    const data = JSON.parse(event.data);
    // 处理不同类型的消息：thinking, progress, fill_field 等
};
```

```java
// 后端流式发送
emitter.send(SseEmitter.event().data(jsonData));
```

### 2. Prompt Engineering

通过精心设计的 System Prompt 和 User Prompt，让 AI 输出结构化的 JSON 数据。

```
System Prompt: 定义 AI 角色和输出格式
User Prompt:   提供具体的文档内容
```

### 3. 数据映射

将 AI 提取的数据映射到前端表单字段，实现自动填充。

---

## 技术架构

```
┌─────────────────────────────────────────────────────────────────┐
│                          浏览器端                                │
│  ┌──────────────┐    SSE    ┌────────────────────────────────┐ │
│  │ loan-form.js │ ←──────── │ 事件监听与UI更新                │ │
│  └──────────────┘            │ - 接收 thinking 事件           │ │
│                              │ - 接收 fill_field 事件         │ │
│                              │ - 高亮当前填充字段              │ │
│                              └────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                  ↕ SSE
┌─────────────────────────────────────────────────────────────────┐
│                         Spring Boot 服务端                      │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ LoanFormController (/api/loan-form/fill-stream)          │  │
│  │  - 创建 SseEmitter                                        │  │
│  │  - 异步处理文档                                            │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              ↓                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ LoanFormService.processDocumentAndFill()                  │  │
│  │                                                            │  │
│  │  1. 读取示例文档                                           │  │
│  │  2. 构建 Prompt → 调用 AI                                  │  │
│  │  3. 解析 AI 返回的 JSON                                    │  │
│  │  4. 逐步发送 SSE 事件：                                     │  │
│  │     - switch_tab: 切换标签页                               │  │
│  │     - fill_field: 填充单个字段                             │  │
│  │     - progress: 更新进度                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              ↓                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Spring AI + ChatModel                                     │  │
│  │  - 发送结构化 Prompt                                       │  │
│  │  - 返回 JSON 格式的提取结果                                │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 代码实现

### 文件结构

```
src/main/java/com/example/aidevelop/
├── controller/
│   └── LoanFormController.java          # SSE 端点
├── service/
│   └── LoanFormService.java             # 核心业务逻辑
└── model/dto/loanform/
    ├── FieldFillRequest.java            # 字段填充请求
    └── LoanFormData.java                # 表单数据模型

src/main/resources/static/
├── loan-form.html                       # 页面结构
├── css/loan-form.css                    # 样式
└── js/loan-form.js                      # 前端逻辑
```

### 1. 后端 SSE 端点

**文件**: `LoanFormController.java`

```java
@GetMapping(value = "/fill-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter fillFormStream(@RequestParam String docId) {
    SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

    // 异步处理，避免阻塞
    new Thread(() -> {
        try {
            loanFormService.processDocumentAndFill(docId, emitter);
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }).start();

    return emitter;
}
```

### 2. AI 提取核心逻辑

**文件**: `LoanFormService.java`

```java
private LoanFormData extractFieldsWithAI(String document, SseEmitter emitter) {
    // System Prompt: 定义 AI 角色和输出格式
    String systemPrompt = """
        你是一个专业的贷款申请信息提取助手。
        请严格按照以下 JSON 格式返回：
        {
          "applicantName": "申请人姓名",
          "idNumber": "身份证号",
          ...
        }
        """;

    // User Prompt: 提供文档内容
    String userPrompt = "请从以下文档中提取信息：\n" + document;

    // 调用 AI
    Prompt prompt = new Prompt(
        new SystemMessage(systemPrompt),
        new UserMessage(userPrompt)
    );

    String response = chatModel.call(prompt).getResult().getOutput().getContent();

    // 解析 JSON
    return objectMapper.readValue(response, LoanFormData.class);
}
```

### 3. 前端 SSE 接收

**文件**: `loan-form.js`

```javascript
function startAiFill() {
    const url = `/api/loan-form/fill-stream?docId=${documentSelect.value}`;
    eventSource = new EventSource(url);

    eventSource.onmessage = (event) => {
        if (isPaused) return; // 支持暂停

        const data = JSON.parse(event.data);
        handleAiMessage(data);
    };
}

function handleAiMessage(data) {
    switch (data.type) {
        case 'thinking':
            addLog(data.content, 'thinking');
            break;
        case 'switch_tab':
            switchTab(data.tabId);
            break;
        case 'fill_field':
            fillField(data.tabId, data.fieldName, data.value);
            break;
        case 'complete':
            stopFilling();
            break;
    }
}
```

### 4. 字段填充动画

**文件**: `loan-form.css`

```css
/* 填充中状态 - 呼吸动画 */
.form-group.filling input {
    border-color: var(--warning-color);
    animation: pulse 1.5s infinite;
}

/* 已填充状态 */
.form-group.filled input {
    border-color: var(--success-color);
    background: #f0fdf4;
}

@keyframes pulse {
    0%, 100% { box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.1); }
    50% { box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.3); }
}
```

---

## AI Prompt 工程

### System Prompt 设计要点

1. **明确角色定位**
   ```
   你是一个专业的贷款申请信息提取助手
   ```

2. **指定输出格式**
   ```
   请严格按照以下 JSON 格式返回
   ```

3. **定义字段映射规则**
   ```
   loanType 映射：个人消费=personal, 企业经营=business...
   ```

4. **处理缺失信息**
   ```
   如果文档中没有某个字段的信息，填空字符串或 0
   ```

5. **约束输出内容**
   ```
   只返回 JSON，不要有其他解释文字
   ```

### User Prompt 设计

```
请从以下贷款申请文档中提取信息：

{文档内容}

请返回 JSON 格式的结构化数据。
```

---

## 运行体验

### 1. 启动应用

```bash
mvn spring-boot:run
```

### 2. 访问页面

```
http://localhost:8080/loan-form.html
```

### 3. 操作步骤

| 步骤 | 操作 | 观察效果 |
|------|------|----------|
| 1 | 从下拉框选择文档 | 文档预览按钮可用 |
| 2 | 点击"预览文档" | 显示完整文档内容 |
| 3 | 点击"开始 AI 填充" | AI 思考日志开始滚动 |
| 4 | 观察 AI 处理 | 进度条逐步更新 |
| 5 | 等待填充完成 | 所有字段自动填充 |
| 6 | 可随时点击"暂停" | 填充停止，可手动编辑 |

### 4. 暂停/继续功能

```javascript
function togglePause() {
    isPaused = !isPaused;
    pauseBtn.textContent = isPaused ? '▶️ 继续' : '⏸️ 暂停';

    if (isPaused) {
        addLog('⏸️ 已暂停，点击继续恢复', 'warning');
    } else {
        addLog('▶️ 继续填充...', 'success');
    }
}
```

---

## 关键知识点

### 1. SSE vs WebSocket

| 特性 | SSE | WebSocket |
|------|-----|-----------|
| 方向 | 单向（服务器→客户端） | 双向 |
| 协议 | HTTP | HTTP + WebSocket 协议 |
| 复杂度 | 简单 | 较复杂 |
| 适用场景 | 服务端推送实时数据 | 双向实时通信 |

本场景使用 SSE 因为只需服务端单向推送填充指令。

### 2. 异步处理

为什么在 `new Thread()` 中处理？

```java
new Thread(() -> {
    loanFormService.processDocumentAndFill(docId, emitter);
}).start();
```

- AI 调用耗时较长（几秒到几十秒）
- 不阻塞 Servlet 线程
- 保持 SSE 连接持续发送数据

### 3. AI 提取的可靠性

**挑战**：AI 可能返回格式错误或不完整的数据

**解决方案**：
```java
// 清理可能的 markdown 代码块标记
if (response.startsWith("```json")) {
    response = response.substring(7);
}
if (response.startsWith("```")) {
    response = response.substring(3);
}
```

### 4. 前后端数据流

```
前端选择 → 后端读取 → AI 提取 → JSON 解析 → 逐步发送 → 前端渲染
   (docId)   (文档内容)   (结构化)    (对象)      (SSE)      (DOM更新)
```

---

## 扩展方向

1. **支持文件上传**
   - 添加文件上传接口
   - 解析 PDF/Word 文档

2. **增强 AI 理解**
   - 使用更强大的模型
   - 添加少样本示例（Few-shot）

3. **人工审核流程**
   - AI 填充后标记不确定字段
   - 要求人工确认

4. **表单验证**
   - 填充完成后自动校验
   - 提示错误字段

5. **保存草稿**
   - 支持保存填写进度
   - 后续继续填写

---

## 总结

这个 demo 展示了 AI 如何通过以下步骤实现智能表单填充：

1. **Prompt Engineering**: 设计让 AI 输出结构化数据的提示词
2. **流式处理**: 使用 SSE 实现实时反馈
3. **前后端协作**: 后端提取、前端渲染的分工配合
4. **人工介入**: 暂停/继续机制保证可控性

这些技术可以应用到各种表单自动化场景，如：
- 保险理赔申请
- 医院病历录入
- 采购订单填写
- 员工入职登记

**核心思想**: 让 AI 处理信息提取和初步填充，让人类专注于审核和决策。
