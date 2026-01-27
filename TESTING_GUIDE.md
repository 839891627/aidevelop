# 功能测试指南

## ✅ 准备工作

### 1. 执行数据库脚本
```bash
mysql -u root -p ai_develop < sql/ai_cost_tracking.sql
```

### 2. 重新编译项目
```bash
mvn clean compile
```

或在 IDEA 中：`Build -> Rebuild Project`

### 3. 启动应用
```bash
mvn spring-boot:run
```

或在 IDEA 中运行 `AiDevelopApplication`

---

## 📋 测试步骤

### 测试 1: 提示词管理 API

#### 1.1 查看当前 System 提示词
```bash
curl http://localhost:8080/api/prompts/system
```

**预期输出**：
```json
{
  "type": "system",
  "content": "## 🎯 角色定位\n...",
  "length": 1234
}
```

#### 1.2 查看提示词状态
```bash
curl http://localhost:8080/api/prompts/status
```

#### 1.3 重新加载提示词
```bash
curl -X POST http://localhost:8080/api/prompts/system/reload
```

---

### 测试 2: 成本追踪功能

#### 2.1 发送聊天消息（触发 AI 调用）
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "query": "你好，请介绍一下你自己"
  }'
```

#### 2.2 查询今日成本统计
```bash
curl http://localhost:8080/api/cost/today
```

**预期输出**：
```json
{
  "startTime": "2026-01-27T00:00:00",
  "endTime": "2026-01-27T15:30:00",
  "totalCost": 0.001234,
  "totalCalls": 5,
  "successCalls": 5,
  "successRate": 100.00,
  "modelUsages": [
    {
      "modelName": "deepseek-chat",
      "provider": "OPENAI",
      "callCount": 5,
      "totalTokens": 1234,
      "totalCost": 0.001234
    }
  ],
  "dailyCosts": [...]
}
```

#### 2.3 查看数据库中的调用日志
```bash
mysql -u root -p ai_develop -e "
  SELECT
    model_name,
    model_type,
    total_tokens,
    cost,
    latency_ms,
    status,
    created_time
  FROM ai_call_log
  ORDER BY created_time DESC
  LIMIT 10;
"
```

---

### 测试 3: 定时任务

#### 3.1 查看日志
定时任务会在每天凌晨1点执行，查看应用日志：

```bash
tail -f logs/aidevelop.log | grep "每日成本统计"
```

**预期输出**：
```
2026-01-27 01:00:00 [scheduling-1] INFO  c.e.a.scheduled.DailyCostStatisticsScheduler - 开始执行每日成本统计任务...
2026-01-27 01:00:01 [scheduling-1] INFO  c.e.a.scheduled.DailyCostStatisticsScheduler - 昨日总调用次数: 123, 成功次数: 120
2026-01-27 01:00:01 [scheduling-1] INFO  c.e.a.scheduled.DailyCostStatisticsScheduler - 模型统计: OPENAI | deepseek-chat | 调用: 123 次 | Token: 12345 | 成本: ¥0.123
2026-01-27 01:00:01 [scheduling-1] INFO  c.e.a.scheduled.DailyCostStatisticsScheduler - 成功率: 97.56%, 平均响应时间: 1234 ms
2026-01-27 01:00:01 [scheduling-1] INFO  c.e.a.scheduled.DailyCostStatisticsScheduler - 每日成本统计任务完成
```

#### 3.2 手动触发（可选）
如需立即测试，可以在 IDEA 中右键点击 `DailyCostStatisticsScheduler` 类，选择 `Run` 方法。

---

### 测试 4: 成本预警

#### 4.1 修改预警阈值
编辑 `DailyCostStatisticsScheduler.java`:
```java
BigDecimal threshold = new BigDecimal("0.01"); // 改为 ¥0.01 测试
```

#### 4.2 等待10分钟或手动触发
每10分钟会自动检查一次今日成本。

#### 4.3 查看预警日志
```bash
tail -f logs/aidevelop.log | grep "成本预警"
```

**预期输出**：
```
2026-01-27 15:40:00 [scheduling-1] WARN  c.e.a.scheduled.DailyCostStatisticsScheduler - ⚠️ 成本预警：今日成本已达到 ¥0.012，超过阈值 ¥0.01
```

---

### 测试 5: 提示词动态更新

#### 5.1 修改提示词文件
编辑 `src/main/resources/prompts/system/default.txt`:
```markdown
## 🎯 角色定位
你是一位专业的金融系统AI助手。
【测试】这是修改后的提示词。
...
```

#### 5.2 重新加载提示词
```bash
curl -X POST http://localhost:8080/api/prompts/system/reload
```

#### 5.3 验证生效
```bash
curl http://localhost:8080/api/prompts/system | grep "测试"
```

#### 5.4 发送测试消息
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"query":"你是谁？"}'
```

AI 的回复应该反映出新的提示词内容。

---

## 🔍 故障排查

### 问题 1: 提示词文件加载失败
**症状**: 提示词显示默认内容

**解决方案**:
1. 检查文件路径是否正确
2. 确认文件编码是 UTF-8
3. 查看应用日志中的错误信息

### 问题 2: 成本记录为 0
**症状**: `totalCost` 为 0

**解决方案**:
1. 检查模型名称是否在定价表中
2. 查看 `AiCallLog` 表中的数据
3. 确认拦截器是否生效（查看日志）

### 问题 3: 定时任务不执行
**症状**: 没有看到定时任务日志

**解决方案**:
1. 确认 `@EnableScheduling` 注解已添加
2. 检查 cron 表达式是否正确
3. 查看是否有异常抛出

### 问题 4: AOP 拦截器不工作
**症状**: `ai_call_log` 表没有数据

**解决方案**:
1. 确认 `spring-boot-starter-aop` 依赖已添加
2. 检查 `@Aspect` 注解是否生效
3. 查看 IDEA 编译是否有警告

---

## 📊 性能影响评估

### 成本追踪的性能开销
- 每次调用额外耗时: ~5-10ms (数据库写入)
- 内存占用: 每条记录 ~500 bytes
- 建议: 生产环境可考虑异步写入

### 优化建议
```java
// 使用 @Async 异步保存日志
@Async
public void saveCallLogAsync(AiCallLog log) {
    aiCallLogRepository.save(log);
}
```

---

## ✅ 验收标准

所有测试通过后，应该满足以下标准：

1. ✅ 提示词可以正常加载和切换
2. ✅ 每次 AI 调用都会被记录到数据库
3. ✅ 成本计算准确（误差 < 5%）
4. ✅ 统计 API 返回正确的数据
5. ✅ 定时任务正常执行
6. ✅ 成本预警正常触发（超过阈值时）

---

## 🎯 下一步

完成测试后，可以考虑：

1. **添加更多模型的定价**
2. **实现告警通知**（邮件/钉钉/企微）
3. **优化性能**（异步写入、批量插入）
4. **添加可视化 Dashboard**
5. **实现提示词 A/B 测试**
