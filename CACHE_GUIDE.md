# 缓存系统使用指南

## ✅ 已完成的缓存功能

### 1. 缓存配置 ✅
- **CacheConfig.java** - 缓存配置类
  - AI 响应缓存（最大 1000 条，30 分钟过期）
  - 向量检索缓存（最大 500 条，1 小时过期）
  - 函数调用缓存（最大 2000 条，10 分钟过期）
  - 默认缓存（最大 10000 条，30 分钟过期）

### 2. 缓存服务 ✅
- **CachedChatService.java** - 带缓存的问答服务
  - 简单问答缓存
  - 带系统提示的问答缓存
  - 缓存清除功能

- **CachedVectorSearchService.java** - 带缓存的向量检索服务
  - 向量检索结果缓存
  - 支持自定义参数
  - 减少重复计算

### 3. 缓存管理 API ✅
- **CacheController.java** - 缓存管理接口
  - `GET /api/cache/stats` - 获取缓存统计
  - `GET /api/cache/names` - 获取所有缓存名称
  - `DELETE /api/cache/{cacheName}` - 清除指定缓存
  - `DELETE /api/cache/all` - 清除所有缓存

### 4. 缓存测试 API ✅
- **CacheTestController.java** - 缓存测试接口
  - `POST /api/cache-test/ask` - 测试问答缓存
  - `DELETE /api/cache-test/ask` - 清除问题缓存
  - `GET /api/cache-test/compare?question=xxx` - 缓存对比测试

---

## 📋 使用指南

### 第一步：重新编译和启动

```bash
# 1. 在 IDEA 中重新构建
Build -> Rebuild Project

# 2. 启动应用
Run AiDevelopApplication
```

### 第二步：测试基础缓存功能

#### 2.1 测试问答缓存
```bash
# 第一次调用（请求 AI，约 1-2 秒）
curl -X POST http://localhost:8080/api/cache-test/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"什么是人工智能？"}'

# 输出示例：
{
  "question": "什么是人工智能？",
  "answer": "人工智能是指...",
  "duration": "1234ms",
  "note": "第二次调用相同问题时会返回缓存，速度更快"
}

# 第二次调用（从缓存读取，约 1-10 毫秒）
curl -X POST http://localhost:8080/api/cache-test/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"什么是人工智能？"}'

# 输出示例：
{
  "question": "什么是人工智能？",
  "answer": "人工智能是指...",
  "duration": "8ms",  # 速度快了很多！
  "note": "第二次调用相同问题时会返回缓存，速度更快"
}
```

#### 2.2 缓存对比测试
```bash
# 自动对比两次调用
curl "http://localhost:8080/api/cache-test/compare?question=什么是AI"

# 输出示例：
{
  "question": "什么是AI",
  "firstCall": {
    "duration": "1234ms",
    "fromCache": false
  },
  "secondCall": {
    "duration": "5ms",
    "fromCache": true,
    "speedup": "246.8x"  # 速度提升了 246 倍！
  },
  "answer": "AI是指..."
}
```

### 第三步：查看缓存统计

```bash
# 查看所有缓存统计
curl http://localhost:8080/api/cache/stats

# 输出示例：
{
  "aiResponse": {
    "description": "AI响应缓存",
    "hitCount": 150,      // 命中次数
    "missCount": 50,      // 未命中次数
    "hitRate": 75.0,      // 命中率 75%
    "evictionCount": 5,   // 被驱逐的条目
    "size": 95            // 当前缓存大小
  },
  "vectorSearch": {
    "description": "向量检索缓存",
    "hitCount": 80,
    "missCount": 20,
    "hitRate": 80.0,
    "evictionCount": 2,
    "size": 45
  },
  "functionCall": {
    "description": "函数调用缓存",
    "hitCount": 200,
    "missCount": 10,
    "hitRate": 95.24,
    "evictionCount": 0,
    "size": 120
  },
  "default": {...}
}
```

### 第四步：清除缓存

```bash
# 清除指定缓存
curl -X DELETE http://localhost:8080/api/cache/aiResponse

# 清除所有缓存
curl -X DELETE http://localhost:8080/api/cache/all

# 清除指定问题的缓存
curl -X DELETE http://localhost:8080/api/cache-test/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"什么是人工智能？"}'
```

---

## 🔧 如何在代码中使用缓存

### 方式 1：使用 @Cacheable 注解

```java
@Service
public class MyService {

    @Cacheable(
        value = "myCache",
        key = "#param",
        cacheManager = "aiResponseCacheManager"
    )
    public String myMethod(String param) {
        // 方法实现
    }
}
```

### 方式 2：使用已实现的缓存服务

```java
@RestController
public class MyController {

    private final CachedChatService cachedChatService;

    @PostMapping("/ask")
    public String ask(@RequestBody String question) {
        // 自动缓存，下次调用相同问题时直接返回
        return cachedChatService.ask(question);
    }
}
```

### 方式 3：清除缓存

```java
@Service
public class MyService {

    @CacheEvict(value = "myCache", key = "#param")
    public void updateData(String param) {
        // 更新数据后自动清除缓存
    }

    @CacheEvict(value = "myCache", allEntries = true)
    public void clearAll() {
        // 清除所有缓存
    }
}
```

---

## 📊 性能优化效果

### 预期性能提升

| 场景 | 无缓存 | 有缓存 | 提升 |
|------|--------|--------|------|
| 重复问答 | 1000-2000ms | 5-10ms | **100-200倍** |
| 向量检索 | 100-300ms | 5-10ms | **20-30倍** |
| 数据库查询 | 50-100ms | 1-5ms | **10-50倍** |

### 成本节省

假设每天有 1000 次重复问答：

- **无缓存**: 1000 次 AI API 调用
  - DeepSeek: ¥0.002/次 × 1000 = ¥2/天
  - 月成本: ¥60

- **有缓存**（命中率 70%）: 300 次 AI API 调用
  - DeepSeek: ¥0.002/次 × 300 = ¥0.6/天
  - 月成本: ¥18

- **节省**: ¥42/月（**70% 节省**）

---

## 🎯 最佳实践

### 1. 缓存键设计

```java
// ✅ 好的缓存键（包含关键参数）
@Cacheable(key = "#userId + ':' + #query")

// ❌ 不好的缓存键（太简单）
@Cacheable(key = "#query")
```

### 2. 缓存过期时间

```java
// AI 响应缓存（30分钟）
.expireAfterWrite(30, TimeUnit.MINUTES)

// 向量检索缓存（1小时）
.expireAfterWrite(1, TimeUnit.HOURS)

// 函数调用缓存（10分钟）
.expireAfterWrite(10, TimeUnit.MINUTES)
```

### 3. 条件缓存

```java
// 只缓存成功的响应
@Cacheable(condition = "#result != null", unless = "#result.isEmpty()")
public String getData(String key) {
    return repository.findByKey(key);
}
```

### 4. 缓存预热

```java
@PostConstruct
public void warmUpCache() {
    // 应用启动时预先加载热点数据
    cachedChatService.ask("什么是人工智能？");
    cachedChatService.ask("如何使用RAG？");
}
```

---

## 🚨 注意事项

### 1. 缓存一致性

**问题**: 数据更新后缓存仍然是旧数据

**解决方案**:
```java
@CacheEvict(value = "myCache", allEntries = true)
public void updateData() {
    // 更新数据后清除缓存
}
```

### 2. 内存占用

**问题**: 缓存过多会导致内存溢出

**解决方案**:
- 设置合理的 `maximumSize`
- 定期清除不常用的缓存
- 监控缓存大小

### 3. 缓存穿透

**问题**: 大量请求不存在的数据，导致缓存失效

**解决方案**:
```java
@Cacheable(value = "myCache", unless = "#result == null")
public String getData(String key) {
    String data = repository.findByKey(key);
    return data;  // null 值不会被缓存
}
```

### 4. 缓存雪崩

**问题**: 大量缓存同时过期，导致请求激增

**解决方案**:
```java
// 随机过期时间，避免同时过期
.expireAfterWrite(30 + Random.nextInt(10), TimeUnit.MINUTES)
```

---

## 📈 监控指标

### 关键指标

1. **命中率**（Hit Rate）
   - 目标: > 70%
   - 公式: 命中次数 / (命中次数 + 未命中次数) × 100%

2. **缓存大小**（Size）
   - 监控: 避免超过最大限制
   - 告警: > 80% 最大容量

3. **驱逐次数**（Eviction Count）
   - 监控: 过高说明缓存空间不足
   - 解决: 增加 maximumSize 或缩短 TTL

### 查看方式

```bash
# 实时查看缓存统计
curl http://localhost:8080/api/cache/stats

# 定期检查（添加到监控脚本）
watch -n 10 'curl -s http://localhost:8080/api/cache/stats | jq'
```

---

## 🎓 进阶使用

### 1. 多级缓存

```java
// L1: 本地缓存（Caffeine）- 快速但容量小
@Cacheable(value = "L1", cacheManager = "caffeineCacheManager")

// L2: Redis 缓存 - 较慢但容量大
@Cacheable(value = "L2", cacheManager = "redisCacheManager")
```

### 2. 缓存刷新

```java
@CachePut(value = "myCache", key = "#param")
public String updateData(String param) {
    // 更新数据并刷新缓存
    return newData;
}
```

### 3. 定时刷新

```java
@Scheduled(cron = "0 0 */6 * * ?")  // 每6小时刷新
public void refreshCache() {
    // 重新加载热点数据到缓存
}
```

---

## ✅ 验收标准

测试完成后，应该满足以下标准：

- [ ] 缓存命中率达到 50% 以上
- [ ] 响应速度提升 10 倍以上
- [ ] 内存占用正常（< 500MB）
- [ ] 缓存统计准确
- [ ] 缓存清除功能正常

---

## 📚 相关文档

- `Caffeine 官方文档`: https://github.com/ben-manes/caffeine
- `Spring Cache 文档`: https://docs.spring.io/spring-framework/reference/integration/cache.html
- `本项目架构文档`: `RAG_ARCHITECTURE.md`

---

**实现日期**: 2026-01-27
**版本**: 1.0.0
**开发者**: Claude Code AI Assistant
