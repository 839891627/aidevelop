package com.example.aidevelop.controller;

import com.example.aidevelop.model.dto.cache.CacheStatsDTO;
import com.example.aidevelop.service.cache.AiCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Tag(name = "缓存调试接口", description = "AI 响应、RAG 检索、工具调用缓存统计与清理")
public class CacheController {

    private final AiCacheService aiCacheService;

    @GetMapping("/stats")
    @Operation(summary = "查看缓存统计", description = "返回每个缓存的 size、hitCount、missCount、hitRate、evictionCount")
    public Map<String, CacheStatsDTO> stats() {
        return aiCacheService.stats();
    }

    @DeleteMapping
    @Operation(summary = "清空全部缓存", description = "清空 AI 响应、RAG 检索、工具调用全部缓存")
    public Map<String, String> clearAll() {
        aiCacheService.clearAll();
        return Map.of("message", "全部缓存已清空");
    }

    @DeleteMapping("/{cacheName}")
    @Operation(summary = "清空指定缓存", description = "支持 aiResponse、ragSearch、toolCall")
    public Map<String, String> clear(
            @Parameter(description = "缓存名称", required = true, example = "aiResponse")
            @PathVariable String cacheName) {
        aiCacheService.clear(cacheName);
        return Map.of("message", "缓存已清空: " + cacheName);
    }
}
