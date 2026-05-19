# 08 - 成本管理与可观测性：AOP 日志、成本统计与缓存策略

## 1. 为什么需要关注 AI 调用成本

- LLM API 按 Token 计费，不加控制会产生高额费用
- 生产环境必须监控：每次调用成本、累计成本、异常调用
- 优化手段：缓存、合理选择模型、控制上下文长度

## 2. AOP 调用日志 (AiCallLoggerAspect)

使用 Spring AOP `@Around` 切面拦截所有 `ChatModel.call()` 和 `EmbeddingModel.embed()` 调用。

核心流程：
1. 记录开始时间，生成 sessionId
2. 从目标对象推断模型名称和提供商
3. 调用 `joinPoint.proceed()` 执行实际方法
4. 成功时提取 Token 信息、计算成本、保存日志
5. 失败时记录错误信息，仍然保存日志，然后抛出原始异常

```java
@Around("execution(* org.springframework.ai.chat.model.ChatModel.call(..))")
public Object logChatCall(ProceedingJoinPoint joinPoint) throws Throwable {
    return logAiCall(joinPoint, ModelType.CHAT);
}

@Around("execution(* org.springframework.ai.embedding.EmbeddingModel.embed(..))")
public Object logEmbeddingCall(ProceedingJoinPoint joinPoint) throws Throwable {
    return logAiCall(joinPoint, ModelType.EMBEDDING);
}
```

关键设计决策：
- **日志保存失败不影响业务**：`saveCallLog()` 内部 try-catch 吞掉异常，只打印 error 日志
- **模型名推断**：通过类名（如 `OpenAi`、`ZhiPuAi`）推断模型和提供商，映射到定价表
- **Token 估算**：当 API 响应中缺少 usage 数据时，按中文字符数/2 粗略估算

## 3. 成本计算 (AiCostCalculator)

按模型定价表计算每次调用费用，输入/输出 Token 分别计价，单位为元/千 Token。

```java
private static final Map<String, ModelPricing> PRICING_TABLE = new HashMap<>();

static {
    PRICING_TABLE.put("deepseek-chat", new ModelPricing(
        new BigDecimal("0.001"),  // 输入
        new BigDecimal("0.002")   // 输出
    ));
    PRICING_TABLE.put("embedding-2", new ModelPricing(
        new BigDecimal("0.0007"),
        new BigDecimal("0.0007")
    ));
}
```

计算公式：

```
输入成本 = (promptTokens / 1000) * inputPrice
输出成本 = (completionTokens / 1000) * outputPrice
总成本 = 输入成本 + 输出成本（保留6位小数）
```

定价参考（以实际官网为准）：

| 模型 | 输入价格(元/千Token) | 输出价格(元/千Token) |
|------|-------------------|-------------------|
| deepseek-chat | 0.001 | 0.002 |
| embedding-2 | 0.0007 | 0.0007 |

未知模型返回 BigDecimal.ZERO，不会报错。添加新模型只需在 static 块中 put 一行。

## 4. 成本统计服务 (AiCostStatisticsService)

聚合查询支持四个时间维度：今日、本周、本月、自定义范围。

统计内容：
- 总调用次数、成功次数、成功率
- 总成本
- 模型使用分布：每个模型的调用次数、Token 用量、成本
- 每日趋势：按天的成本和调用次数

返回数据结构：

```java
public record CostStats(
    LocalDateTime startTime,
    LocalDateTime endTime,
    BigDecimal totalCost,
    long totalCalls,
    long successCalls,
    BigDecimal successRate,          // 百分比
    List<ModelUsage> modelUsages,
    List<DailyCost> dailyCosts
) {}

public record ModelUsage(
    String modelName, String provider,
    long callCount, long totalTokens, BigDecimal totalCost
) {}

public record DailyCost(
    LocalDate date, BigDecimal cost, long callCount
) {}
```

底层依赖 `AiCallLogRepository` 的 JPQL 聚合查询，按模型分组统计和按日期分组统计各一条查询。

## 5. 成本管理前端

- `cost.html`：成本看板页面，包含时间段选择器、统计卡片、每日趋势表格、模型分布
- `cost.js`：`CostManager` 类，通过 fetch 调用 `/api/cost/*` 接口获取数据
- `cost.css`：独立样式

页面结构：
1. **时间段选择**：今日 / 本周 / 本月 / 自定义（自定义弹出日期选择器）
2. **统计卡片**：总调用次数、总成本、成功/总调用比、成功率
3. **每日成本趋势表格**：按天展示成本和调用次数
4. **模型分布**：按模型展示调用次数、Token 用量、成本

从聊天页（index.html）导航栏可直接跳转到成本页面。

## 6. 定时任务 (DailyCostStatisticsScheduler)

使用 `@Scheduled` 注解实现两个定时任务：

| 任务 | cron 表达式 | 说明 |
|------|-----------|------|
| 每日统计 | `0 0 1 * * ?`（凌晨1点） | 汇总昨日各模型的调用次数、Token、成本，输出日志 |
| 成本预警 | `0 */10 * * * ?`（每10分钟） | 检查今日累计成本是否超过阈值（默认100元），超限打印 warn 日志 |

预警阈值硬编码为 `new BigDecimal("100")`，可根据需要调整或改为配置项。

扩展方向：将统计结果持久化到 `ai_daily_cost_stats` 表，或接入邮件/钉钉/企微通知。

## 7. 多级缓存策略

### 7.1 缓存架构

项目内置三个独立的 Caffeine 本地缓存，由 `AiCacheService` 统一读写，并通过 `/api/cache/stats` 暴露命中统计：

| 缓存类型 | 名称 | TTL | 最大条目 | 适用场景 |
|---------|----------|-----|---------|---------|
| AI 响应缓存 | aiResponse | 30 分钟 | 1000 | 单轮重复问题的缓存命中 |
| RAG 检索缓存 | ragSearch | 1 小时 | 500 | 相同查询的检索结果 |
| 工具调用缓存 | toolCall | 10 分钟 | 2000 | 频繁查询的贷款/还款/风险信息 |

### 7.2 Caffeine Cache 特点

- 本地内存缓存，零网络开销
- 支持 TTL（expireAfterWrite）、容量上限（maximumSize）、LRU 淘汰
- 适合单机部署场景，多实例部署需替换为 Redis

### 7.3 缓存 Key 设计

- AI 响应：`message + model + temperature + maxTokens + routeType + rag 参数`
- RAG 检索：`operation + query + type + topK + threshold`
- 函数调用：`tool + method + userNo/status/bizSerial`

AI 响应缓存只覆盖 `conversationId` 为空、且未进入显式工具路由（非 `TOOL_ONLY`）的阻塞式 `/api/chat` 单轮请求；流式接口不缓存，明确业务查询类请求不缓存，避免动态业务数据误命中。

RAG 缓存覆盖：

- `/api/rag/search`
- `/api/rag/hybrid-search`
- `/api/rag/rerank-search`
- `/api/rag/pipeline`（仅 `conversationId` 为空时）

工具缓存加在 `LoanQueryFunction`、`RepaymentQueryFunction`、`RiskAssessmentFunction`，因此 Function Calling 和 Agent Tool 复用同一套缓存。

### 7.4 缓存调试接口

```http
### 查看缓存统计
GET http://localhost:8080/api/cache/stats

### 清空全部缓存
DELETE http://localhost:8080/api/cache

### 清空指定缓存
DELETE http://localhost:8080/api/cache/aiResponse
DELETE http://localhost:8080/api/cache/ragSearch
DELETE http://localhost:8080/api/cache/toolCall
```

## 8. AiCallLog 数据模型

```java
@Entity
@Table(name = "ai_call_log", indexes = {
    @Index(name = "idx_session", columnList = "session_id"),
    @Index(name = "idx_user", columnList = "user_id"),
    @Index(name = "idx_model", columnList = "model_name"),
    @Index(name = "idx_created", columnList = "created_time"),
    @Index(name = "idx_provider", columnList = "provider")
})
public class AiCallLog {
    private Long id;
    private String sessionId;
    private String userId;
    private String modelName;
    private String modelType;      // CHAT / EMBEDDING
    private String provider;       // OPENAI / ZHIPUAI / ANTHROPIC
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private BigDecimal cost;       // 本次调用成本（元），精度 10,6
    private Long latencyMs;        // 响应耗时（毫秒）
    private String status;         // SUCCESS / FAILURE / TIMEOUT
    private String errorMessage;
    private LocalDateTime createdTime;  // @PrePersist 自动填充
}
```

五个索引覆盖了按会话查询、按用户查询、按模型查询、按时间范围查询、按提供商查询的场景。

## 9. API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/cost/today` | 今日成本统计 |
| GET | `/api/cost/week` | 本周成本统计 |
| GET | `/api/cost/month` | 本月成本统计 |
| GET | `/api/cost/range?start=&end=` | 自定义时间范围（ISO DATE_TIME 格式） |

所有接口返回 `CostStats` record，包含统计概要、模型使用分布、每日成本趋势。

## 10. 动手实验

1. 发送多次相同的单轮聊天消息，观察响应中的 `cacheHit` 和 `cacheType`
2. 修改 `AiCostCalculator` 的 `PRICING_TABLE`，添加新模型定价，观察成本计算变化
3. 添加一个新的统计维度：在 Repository 中按 `sessionId` 分组统计成本
4. 实现成本预警：当单次调用成本超过 1 元时，在 AOP 切面中打印 warn 日志

## 11. 关键代码文件

| 文件 | 说明 |
|------|------|
| `src/main/java/.../interceptor/AiCallLoggerAspect.java` | AOP 调用日志切面 |
| `src/main/java/.../service/cost/AiCostCalculator.java` | 成本计算器 |
| `src/main/java/.../service/cost/AiCostStatisticsService.java` | 成本统计服务 |
| `src/main/java/.../controller/AiCostController.java` | 成本统计 API |
| `src/main/java/.../scheduled/DailyCostStatisticsScheduler.java` | 定时统计与预警 |
| `src/main/java/.../config/CacheConfig.java` | Caffeine 缓存配置 |
| `src/main/java/.../service/cache/AiCacheService.java` | 统一缓存读写、清理、统计 |
| `src/main/java/.../controller/CacheController.java` | 缓存调试 API |
| `src/main/java/.../model/entity/AiCallLog.java` | 调用日志实体 |
| `src/main/java/.../repository/AiCallLogRepository.java` | 日志数据访问层 |
| `src/main/resources/static/cost.html` | 成本看板页面 |
| `src/main/resources/static/js/cost.js` | 成本看板逻辑 |
| `src/main/resources/static/css/cost.css` | 成本看板样式 |
