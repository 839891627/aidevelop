package com.example.aidevelop.service.cache;

import com.example.aidevelop.config.AppCacheProperties;
import com.example.aidevelop.model.dto.cache.CacheStatsDTO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiCacheService {

    public static final String AI_RESPONSE = "aiResponse";
    public static final String RAG_SEARCH = "ragSearch";
    public static final String TOOL_CALL = "toolCall";

    private final AppCacheProperties properties;
    private final Map<String, Cache<String, Object>> caches;

    public AiCacheService(
            AppCacheProperties properties,
            @Qualifier(AI_RESPONSE) Cache<String, Object> aiResponseCache,
            @Qualifier(RAG_SEARCH) Cache<String, Object> ragSearchCache,
            @Qualifier(TOOL_CALL) Cache<String, Object> toolCallCache) {
        this.properties = properties;
        this.caches = new LinkedHashMap<>();
        this.caches.put(AI_RESPONSE, aiResponseCache);
        this.caches.put(RAG_SEARCH, ragSearchCache);
        this.caches.put(TOOL_CALL, toolCallCache);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String cacheName, String key) {
        if (!isEnabled(cacheName)) {
            return Optional.empty();
        }
        Object value = resolve(cacheName).getIfPresent(key);
        if (value != null) {
            log.debug("缓存命中: cache={}, key={}", cacheName, key);
        } else {
            log.debug("缓存未命中: cache={}, key={}", cacheName, key);
        }
        return Optional.ofNullable((T) value);
    }

    public void put(String cacheName, String key, Object value) {
        if (!isEnabled(cacheName) || value == null) {
            return;
        }
        resolve(cacheName).put(key, value);
        log.debug("写入缓存: cache={}, key={}", cacheName, key);
    }

    public void clear(String cacheName) {
        resolve(cacheName).invalidateAll();
        resolve(cacheName).cleanUp();
        log.info("已清空缓存: {}", cacheName);
    }

    public void clearAll() {
        caches.keySet().forEach(this::clear);
    }

    public Map<String, CacheStatsDTO> stats() {
        Map<String, CacheStatsDTO> result = new LinkedHashMap<>();
        caches.forEach((cacheName, cache) -> {
            CacheStats stats = cache.stats();
            result.put(cacheName, new CacheStatsDTO(
                cacheName,
                isEnabled(cacheName),
                cache.estimatedSize(),
                stats.hitCount(),
                stats.missCount(),
                roundHitRate(stats),
                stats.evictionCount()
            ));
        });
        return result;
    }

    private Cache<String, Object> resolve(String cacheName) {
        Cache<String, Object> cache = caches.get(cacheName);
        if (cache == null) {
            throw new IllegalArgumentException("未知缓存名称: " + cacheName);
        }
        return cache;
    }

    private boolean isEnabled(String cacheName) {
        if (!properties.isEnabled()) {
            return false;
        }
        return switch (cacheName) {
            case AI_RESPONSE -> properties.getAiResponse().isEnabled();
            case RAG_SEARCH -> properties.getRagSearch().isEnabled();
            case TOOL_CALL -> properties.getToolCall().isEnabled();
            default -> false;
        };
    }

    private double roundHitRate(CacheStats stats) {
        if (stats.requestCount() == 0) {
            return 0.0;
        }
        return round(stats.hitRate());
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
