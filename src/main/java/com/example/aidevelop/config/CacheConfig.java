package com.example.aidevelop.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类
 * 使用 Caffeine 作为本地缓存实现
 */
@Slf4j
@Configuration
@EnableCaching  // 启用 Spring Cache 注解
public class CacheConfig {

    /**
     * AI 响应缓存配置
     * - 容量: 1000 条
     * - 过期时间: 30 分钟
     * - 作用: 缓存相同问题的 AI 回答，避免重复调用
     */
    @Bean
    public CacheManager aiResponseCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
        );
        System.out.println("AI 响应缓存已初始化: 最大1000条, 30分钟过期");
        return cacheManager;
    }

    /**
     * 向量检索缓存配置
     * - 容量: 500 条
     * - 过期时间: 1 小时
     * - 作用: 缓存向量检索结果
     */
    @Bean
    public CacheManager vectorSearchCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(1, TimeUnit.HOURS)
        );
        System.out.println("向量检索缓存已初始化: 最大500条, 1小时过期");
        return cacheManager;
    }

    /**
     * 函数调用结果缓存配置
     * - 容量: 2000 条
     * - 过期时间: 10 分钟
     * - 作用: 缓存数据库查询结果
     */
    @Bean
    public CacheManager functionCallCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
        );
        System.out.println("函数调用缓存已初始化: 最大2000条, 10分钟过期");
        return cacheManager;
    }

    /**
     * 默认缓存管理器（主要使用）
     */
    @Bean
    @Primary
    public CacheManager defaultCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
        );
        System.out.println("默认缓存已初始化: 最大10000条, 30分钟过期");
        return cacheManager;
    }
}
