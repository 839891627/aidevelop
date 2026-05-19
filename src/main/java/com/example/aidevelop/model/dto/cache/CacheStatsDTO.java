package com.example.aidevelop.model.dto.cache;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "缓存统计")
public record CacheStatsDTO(
    @Schema(description = "缓存名称", example = "aiResponse")
    String cacheName,
    @Schema(description = "缓存是否启用", example = "true")
    boolean enabled,
    @Schema(description = "当前缓存条目估算数量", example = "12")
    long size,
    @Schema(description = "命中次数", example = "8")
    long hitCount,
    @Schema(description = "未命中次数", example = "3")
    long missCount,
    @Schema(description = "命中率", example = "0.7273")
    double hitRate,
    @Schema(description = "淘汰次数", example = "0")
    long evictionCount
) {
}
