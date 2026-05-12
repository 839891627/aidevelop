# 05 - Function Calling：让 AI 调用后端工具

## 1. 核心概念

### 什么是 Function Calling

Function Calling 让 LLM 能够**调用外部工具**。LLM 不直接执行代码，而是：
1. 分析用户意图
2. 决定是否需要调用工具
3. 选择调用哪个工具
4. 生成调用参数
5. 接收工具返回的结果
6. 将结果组织成自然语言回答

这是 AI Agent 系统的基础能力 -- Agent 的核心就是"感知环境 + 选择工具 + 执行行动"。

### 交互流程

```
用户: "帮我查一下贷款 LN001 的信息"
        │
        ▼
  ┌─────────────┐
  │  ChatClient  │ ──→ LLM 分析：需要查贷款数据
  └─────────────┘
        │ LLM 选择 loanQueryFunction，生成参数 {loanNo: "LN001"}
        ▼
  ┌──────────────────┐
  │ LoanQueryFunction │ ──→ SELECT * FROM loan WHERE loan_no = 'LN001'
  └──────────────────┘
        │ 返回贷款数据给 LLM
        ▼
  ┌─────────────┐
  │  ChatClient  │ ──→ LLM 组织自然语言回答
  └─────────────┘
        │
        ▼
"贷款 LN001 的信息：金额 50,000 元，状态为正常还款..."
```

### Spring AI 的实现方式

- 函数是标准的 Spring Bean，实现 `Function<I, O>` 接口
- 通过 `@Component` 注册到容器，通过 `@Description` 描述功能
- 在 ChatClient 构建时通过 `defaultFunctions()` 注册
- Spring AI 自动将函数描述发送给 LLM，LLM 自主决定调用

## 2. 三个函数详解

### 2.1 贷款查询函数 (LoanQueryFunction)

**功能**：根据贷款编号查询贷款详情

```java
@Component
@Description("查询贷款信息，包括贷款金额、状态、逾期天数等")
public class LoanQueryFunction implements Function<LoanQueryFunction.Request, LoanQueryFunction.Response> {

    private final LoanRepository loanRepository;

    @Override
    public Response apply(Request request) {
        Loan loan = loanRepository.findByLoanNo(request.loanNo());
        // 转换为 Response 返回
    }

    public record Request(String loanNo) {}
    public record Response(String loanNo, BigDecimal amount, String status, ...) {}
}
```

**关键点**：
- Request/Response 用 record 定义，结构清晰
- Spring AI 会自动将 record 的字段描述发送给 LLM
- LLM 根据字段名和函数描述决定是否调用

### 2.2 还款查询函数 (RepaymentQueryFunction)

**功能**：查询某笔贷款的还款记录

结构与 LoanQueryFunction 类似，查询的是 `repayment_record` 表。

### 2.3 风险评估函数 (RiskAssessmentFunction) -- 最值得学习

**功能**：综合贷款和还款数据，评估客户风险等级

这是三个函数中最复杂的，展示了 Function Calling 的真正价值 -- **让 LLM 调用包含复杂业务逻辑的函数**。

```
风险评估逻辑：
1. 查询贷款信息 -> 获取贷款金额、逾期天数
2. 查询还款记录 -> 获取逾期次数、待还款金额
3. 规则计算：
   - 逾期次数 >= 3        → HIGH
   - 逾期次数 >= 1        → MEDIUM
   - 贷款天数 > 180 且无逾期 → LOW
   - 其他                  → LOW（偏保守）
4. 生成风险描述和改进建议
```

**关键点**：
- 函数内部可以组合多个数据源（LoanRepository + RepaymentRecordRepository）
- 业务逻辑完全在 Java 代码中，LLM 只负责决定"是否调用"
- 返回结构化数据（riskLevel + description + recommendations），LLM 负责组织语言

## 3. 函数注册

在 `AiModelConfig.java` 中注册：

```java
@Bean
public ChatClient chatClient(ChatModel chatModel, ...) {
    return ChatClient.builder(chatModel)
        .defaultSystem(promptService.getSystemPrompt())
        .defaultAdvisors(
            new QuestionAnswerAdvisor(vectorStore)
        )
        .defaultFunctions(
            "loanQueryFunction",        // Bean 名称
            "repaymentQueryFunction",
            "riskAssessmentFunction"
        )
        .build();
}
```

`defaultFunctions()` 接收的是 Spring Bean 名称。Spring AI 会：
1. 从容器中获取对应的 Bean
2. 读取 `@Description` 注解的描述
3. 分析 `Function<Request, Response>` 的 record 字段
4. 将这些信息作为工具定义发送给 LLM API

## 4. 添加新函数的步骤

以添加"客户信息查询函数"为例：

### 步骤 1：创建实体和仓库

```java
@Entity
@Table(name = "customer")
public class Customer {
    @Id private Long id;
    private String name;
    private String idCard;
    private Integer creditScore;
}

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Customer findByIdCard(String idCard);
}
```

### 步骤 2：创建函数类

```java
@Component
@Description("查询客户信息，包括姓名、身份证号、信用评分")
public class CustomerQueryFunction implements Function<CustomerQueryFunction.Request, CustomerQueryFunction.Response> {

    private final CustomerRepository customerRepository;

    @Override
    public Response apply(Request request) {
        Customer customer = customerRepository.findByIdCard(request.idCard());
        return new Response(customer.getName(), customer.getIdCard(), customer.getCreditScore());
    }

    public record Request(@Description("客户身份证号") String idCard) {}
    public record Response(String name, String idCard, Integer creditScore) {}
}
```

### 步骤 3：注册到 ChatClient

在 `AiModelConfig.java` 的 `defaultFunctions()` 中添加 `"customerQueryFunction"`。

### 步骤 4：测试

在聊天界面输入："帮我查一下身份证号 320xxx 的客户信息"，AI 会自动调用新函数。

## 5. Function Calling 的局限和最佳实践

### 局限
- LLM 可能**错误调用**函数（选错函数或参数不对）
- 函数返回的数据量受 Token 限制
- 不适合实时性要求极高的场景（LLM 推理有延迟）

### 最佳实践
- 函数描述要**准确具体**，避免歧义
- Request 字段加 `@Description` 注解帮助 LLM 理解参数含义
- 函数返回**结构化数据**而非长文本，让 LLM 组织语言
- 对敏感操作（如修改数据）添加确认机制

## 6. 动手实验

### 实验 1：测试 Function Calling

在聊天界面依次输入，观察 AI 如何选择函数：

```
"帮我查一下贷款 LN001"               → loanQueryFunction
"LN001 的还款记录是什么"              → repaymentQueryFunction
"评估一下 LN001 的风险"               → riskAssessmentFunction
"贷款逾期了怎么办"                    → 不调用函数，用 RAG 知识回答
"查一下 LN001 的信息并评估风险"       → 可能连续调用多个函数
```

### 实验 2：添加新函数

按照第 4 节的步骤，添加一个 `CustomerQueryFunction`。

### 实验 3（挑战）：组合函数

创建一个 `LoanSummaryFunction`，内部同时调用 LoanRepository 和 RepaymentRecordRepository，返回贷款综合报告（本金、已还、待还、逾期情况）。

## 7. 关键代码文件

| 文件 | 关注点 |
|------|--------|
| `service/function/LoanQueryFunction.java` | 简单查询函数模板 |
| `service/function/RepaymentQueryFunction.java` | 列表查询函数 |
| `service/function/RiskAssessmentFunction.java` | 复杂业务逻辑函数 |
| `config/AiModelConfig.java` | 函数注册到 ChatClient |
| `model/entity/Loan.java` | 贷款实体 |
| `model/entity/RepaymentRecord.java` | 还款记录实体 |
| `repository/LoanRepository.java` | 贷款数据访问 |
| `repository/RepaymentRecordRepository.java` | 还款数据访问 |
