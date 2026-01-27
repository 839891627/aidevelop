# 成本追踪与提示词管理 - 实现完成

## ✅ 已完成的功能

### 1. 成本追踪系统

#### 1.1 数据库表
- ✅ `ai_call_log` - AI 调用日志表
- ✅ `ai_daily_cost_stats` - 每日成本统计表（预创建，用于加速查询）

#### 1.2 核心组件
- ✅ `AiCallLog` - 调用日志实体
- ✅ `AiCallLogRepository` - 数据访问层
- ✅ `AiCostCalculator` - 成本计算服务
  - 支持 DeepSeek (deepseek-chat)
  - 支持智谱AI (embedding-2)
  - 可扩展其他模型
- ✅ `AiCostStatisticsService` - 统计服务
- ✅ `AiCostController` - 统计 API

#### 1.3 统计维度
- 今日/本周/本月成本统计
- 每个模型的使用情况（调用次数、token数、成本）
- 每日成本趋势
- 成功率统计

---

### 2. 提示词管理系统

#### 2.1 文件结构
```
resources/prompts/
├── system/
│   └── default.txt  # 默认系统提示词
├── rag/
│   └── qa.txt       # RAG 问答提示词（预留）
└── function/
    └── calling.txt  # Function Calling 提示词（预留）
```

#### 2.2 核心组件
- ✅ `PromptProperties` - 提示词配置属性
- ✅ `PromptService` - 提示词服务
  - 从文件加载提示词
  - 支持运行时重新加载
  - 降级策略（文件不存在时使用默认提示词）
- ✅ `AiModelConfig` - 已改造使用 PromptService

#### 2.3 配置文件
```yaml
app:
  prompts:
    enabled: true
    base-path: classpath:prompts/
    system-prompt: system/default.txt
    rag-qa-prompt: rag/qa.txt
    function-calling-prompt: function/calling.txt
```

---

## 📋 使用指南

### 第一步：执行数据库脚本

```bash
# 创建成本追踪表
mysql -u root -p ai_develop < sql/ai_cost_tracking.sql
```

### 第二步：重新编译和启动

```bash
# 在 IDEA 中重新构建项目
Build -> Rebuild Project

# 启动应用
```

### 第三步：测试成本统计 API

```bash
# 今日成本统计
curl http://localhost:8080/api/cost/today

# 本周成本统计
curl http://localhost:8080/api/cost/week

# 本月成本统计
curl http://localhost:8080/api/cost/month

# 指定时间范围统计
curl "http://localhost:8080/api/cost/range?start=2026-01-01T00:00:00&end=2026-01-31T23:59:59"
```

### 第四步：测试提示词管理

访问 http://localhost:8080/api/chat 发送消息，系统会自动使用 `resources/prompts/system/default.txt` 中的提示词。

**修改提示词**：
1. 编辑 `src/main/resources/prompts/system/default.txt`
2. 重新编译应用
3. 重启后生效

**运行时重新加载提示词**（需要先实现 API）：
```bash
# TODO: 添加重新加载提示词的 API
curl -X POST http://localhost:8080/api/prompts/reload
```

---

## 🔧 后续需要补充的功能

### 1. 成本追踪部分

#### 1.1 AI 调用拦截器
**状态**: ❌ 未实现

**需要**: 创建一个拦截器或切面，自动记录每次 AI 调用

**实现方式**:
```java
@Aspect
@Component
public class AiCallLoggerAspect {
    @Around("execution(* org.springframework.ai.chat.model.ChatModel.call(..))")
    public Object logAiCall(ProceedingJoinPoint joinPoint) {
        // 记录调用前时间
        // 执行调用
        // 记录调用后时间、token使用
        // 计算成本
        // 保存到数据库
    }
}
```

#### 1.2 定时统计任务
**状态**: ❌ 未实现

**需要**: 每天凌晨统计昨天的成本数据，写入 `ai_daily_cost_stats` 表

**实现方式**:
```java
@Scheduled(cron = "0 0 1 * * ?")  // 每天凌晨1点
public void calculateDailyStats() {
    // 统计昨天的数据
    // 写入统计表
}
```

#### 1.3 成本预警
**状态**: ❌ 未实现

**需要**: 当成本超过阈值时发送告警

**实现方式**:
```java
@Scheduled(cron = "0 */10 * * * ?")  // 每10分钟检查一次
public void checkCostThreshold() {
    BigDecimal todayCost = statisticsService.getTodayStats().totalCost();
    if (todayCost.compareTo(new BigDecimal("100")) > 0) {
        // 发送告警
    }
}
```

### 2. 提示词管理部分

#### 2.1 提示词管理 API
**状态**: ❌ 未实现

**需要**: 提供 API 来查看、切换、重新加载提示词

**实现方式**:
```java
@RestController
@RequestMapping("/api/prompts")
public class PromptController {

    @GetMapping("/system")
    public String getSystemPrompt() {
        return promptService.getSystemPrompt();
    }

    @PostMapping("/reload")
    public String reloadPrompt() {
        return promptService.reloadSystemPrompt();
    }
}
```

#### 2.2 A/B 测试支持
**状态**: ❌ 未实现

**需要**: 支持多个提示词版本，随机分配

**实现方式**:
```java
@Service
public class PromptAbTestService {

    public String getSystemPrompt(String userId) {
        // 根据用户ID哈希值选择版本
        int version = userId.hashCode() % 2;
        return version == 0
            ? promptService.getSystemPrompt("v1.txt")
            : promptService.getSystemPrompt("v2.txt");
    }
}
```

#### 2.3 提示词效果分析
**状态**: ❌ 未实现

**需要**: 跟踪不同提示词版本的效果（满意度、成功率）

**实现方式**:
```sql
CREATE TABLE prompt_performance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    prompt_version VARCHAR(10),
    user_satisfaction_score INT,
    success_rate DECIMAL(5, 4),
    avg_response_time_ms BIGINT,
    stats_date DATE
);
```

---

## 📊 模型定价配置

### 当前配置

| 模型 | 类型 | 输入定价 | 输出定价 | 来源 |
|------|------|----------|----------|------|
| deepseek-chat | Chat | ¥0.001/千tokens | ¥0.002/千tokens | https://api.deepseek.com/pricing |
| embedding-2 | Embedding | ¥0.0007/千tokens | ¥0.0007/千tokens | https://open.bigmodel.cn/pricing |

### 添加新模型

在 `AiCostCalculator.java` 中添加：

```java
static {
    // 已有模型...

    // 新增模型
    PRICING_TABLE.put("gpt-4-turbo", new ModelPricing(
        new BigDecimal("0.01"),   // 输入定价
        new BigDecimal("0.03")    // 输出定价
    ));
}
```

---

## 🎯 下一步建议

### 优先级 1（必须完成）
1. ✅ 执行数据库脚本
2. ✅ 测试成本统计 API
3. ✅ 测试提示词加载
4. ❌ 实现 AI 调用拦截器（自动记录）
5. ❌ 实现定时统计任务

### 优先级 2（推荐完成）
6. ❌ 添加成本预警
7. ❌ 实现提示词管理 API
8. ❌ 添加提示词效果分析

### 优先级 3（可选）
9. ❌ 实现 A/B 测试
10. ❌ 添加成本可视化 Dashboard
11. ❌ 支持数据库存储提示词

---

## 📝 相关文件

### 成本追踪
- `sql/ai_cost_tracking.sql` - 数据库表
- `src/main/java/com/example/aidevelop/model/entity/AiCallLog.java`
- `src/main/java/com/example/aidevelop/repository/AiCallLogRepository.java`
- `src/main/java/com/example/aidevelop/service/cost/AiCostCalculator.java`
- `src/main/java/com/example/aidevelop/service/cost/AiCostStatisticsService.java`
- `src/main/java/com/example/aidevelop/controller/AiCostController.java`

### 提示词管理
- `src/main/resources/prompts/` - 提示词文件目录
- `src/main/resources/prompts/system/default.txt` - 默认系统提示词
- `src/main/java/com/example/aidevelop/config/PromptProperties.java`
- `src/main/java/com/example/aidevelop/service/prompt/PromptService.java`
- `src/main/java/com/example/aidevelop/config/AiModelConfig.java` (已修改)

---

## ⚠️ 注意事项

1. **成本定价更新**: AI 服务商的定价可能随时调整，需要定期更新 `AiCostCalculator` 中的定价表

2. **Token 计算**: 不同模型的 token 计算方式可能不同，需要确保准确性

3. **提示词文件编码**: 确保提示词文件使用 UTF-8 编码

4. **提示词版本管理**: 建议使用 Git 管理提示词文件的版本

5. **性能影响**: 每次调用都记录日志会有一定的性能开销，可以考虑异步写入

---

## 🚀 快速开始

```bash
# 1. 创建数据库表
mysql -u root -p ai_develop < sql/ai_cost_tracking.sql

# 2. 在 IDEA 中重新构建
Build -> Rebuild Project

# 3. 启动应用

# 4. 测试成本统计
curl http://localhost:8080/api/cost/today

# 5. 测试聊天（提示词）
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"query":"你好"}'

# 6. 查看成本统计
curl http://localhost:8080/api/cost/today
```
