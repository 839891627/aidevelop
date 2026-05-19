package com.example.aidevelop.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean("aiResponse")
    public Cache<String, Object> aiResponseCache(AppCacheProperties properties) {
        return buildCache(properties.getAiResponse());
    }

    @Bean("ragSearch")
    public Cache<String, Object> ragSearchCache(AppCacheProperties properties) {
        return buildCache(properties.getRagSearch());
    }

    @Bean("toolCall")
    public Cache<String, Object> toolCallCache(AppCacheProperties properties) {
        return buildCache(properties.getToolCall());
    }

    private Cache<String, Object> buildCache(AppCacheProperties.CacheSpec spec) {
        long ttlMinutes = spec.getTtlMinutes() > 0 ? spec.getTtlMinutes() : 30;
        long maximumSize = spec.getMaximumSize() > 0 ? spec.getMaximumSize() : 1000;
        return Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(ttlMinutes))
            .maximumSize(maximumSize)
            .recordStats()
            .build();
    }
}
