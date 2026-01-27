package com.example.aidevelop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存管理 API
 */
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Tag(name = "缓存管理", description = "缓存查看和管理相关 API")
public class CacheController {

    private final CacheManager defaultCacheManager;
    private final CacheManager aiResponseCacheManager;
    private final CacheManager vectorSearchCacheManager;
    private final CacheManager functionCallCacheManager;

    /**
     * 获取所有缓存名称
     */
    @GetMapping("/names")
    @Operation(summary = "获取所有缓存名称", description = "获取系统中所有缓存的名称列表")
    public Map<String, Object> getCacheNames() {
        Map<String, Object> result = new HashMap<>();

        result.put("default", defaultCacheManager.getCacheNames());
        result.put("aiResponse", aiResponseCacheManager.getCacheNames());
        result.put("vectorSearch", vectorSearchCacheManager.getCacheNames());
        result.put("functionCall", functionCallCacheManager.getCacheNames());

        return result;
    }

    /**
     * 清除指定名称的缓存
     */
    @DeleteMapping("/{cacheName}")
    @Operation(summary = "清除指定缓存", description = "清除指定缓存名称的所有缓存项")
    public Map<String, Object> clearCache(@PathVariable String cacheName) {
        Map<String, Object> result = new HashMap<>();

        try {
            CacheManager cacheManager = getCacheManager(cacheName);
            if (cacheManager != null) {
                cacheManager.getCacheNames().forEach(name -> {
                    cacheManager.getCache(name).clear();
                });
                result.put("success", true);
                result.put("message", "缓存已清除: " + cacheName);
            } else {
                result.put("success", false);
                result.put("message", "未找到缓存: " + cacheName);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "清除缓存失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 清除所有缓存
     */
    @DeleteMapping("/all")
    @Operation(summary = "清除所有缓存", description = "清除系统中所有缓存的缓存项")
    public Map<String, Object> clearAllCaches() {
        Map<String, Object> result = new HashMap<>();

        try {
            defaultCacheManager.getCacheNames().forEach(name -> {
                defaultCacheManager.getCache(name).clear();
            });
            aiResponseCacheManager.getCacheNames().forEach(name -> {
                aiResponseCacheManager.getCache(name).clear();
            });
            vectorSearchCacheManager.getCacheNames().forEach(name -> {
                vectorSearchCacheManager.getCache(name).clear();
            });
            functionCallCacheManager.getCacheNames().forEach(name -> {
                functionCallCacheManager.getCache(name).clear();
            });

            result.put("success", true);
            result.put("message", "所有缓存已清除");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "清除缓存失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取缓存统计（简化版）
     */
    @GetMapping("/stats")
    @Operation(summary = "获取缓存统计", description = "获取所有缓存的基本信息")
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("aiResponse", Map.of(
            "description", "AI响应缓存",
            "cacheNames", aiResponseCacheManager.getCacheNames()
        ));

        stats.put("vectorSearch", Map.of(
            "description", "向量检索缓存",
            "cacheNames", vectorSearchCacheManager.getCacheNames()
        ));

        stats.put("functionCall", Map.of(
            "description", "函数调用缓存",
            "cacheNames", functionCallCacheManager.getCacheNames()
        ));

        stats.put("default", Map.of(
            "description", "默认缓存",
            "cacheNames", defaultCacheManager.getCacheNames()
        ));

        return stats;
    }

    /**
     * 根据缓存名称获取对应的 CacheManager
     */
    private CacheManager getCacheManager(String cacheName) {
        return switch (cacheName) {
            case "aiResponse", "ai" -> aiResponseCacheManager;
            case "vectorSearch", "vector" -> vectorSearchCacheManager;
            case "functionCall", "function" -> functionCallCacheManager;
            case "default" -> defaultCacheManager;
            default -> null;
        };
    }
}
