package com.example.aidevelop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.cache")
public class AppCacheProperties {

    private boolean enabled = true;
    private CacheSpec aiResponse = CacheSpec.of(30, 1000);
    private CacheSpec ragSearch = CacheSpec.of(60, 500);
    private CacheSpec toolCall = CacheSpec.of(10, 2000);

    @Data
    public static class CacheSpec {
        private boolean enabled = true;
        private long ttlMinutes;
        private long maximumSize;

        public static CacheSpec of(long ttlMinutes, long maximumSize) {
            CacheSpec spec = new CacheSpec();
            spec.setTtlMinutes(ttlMinutes);
            spec.setMaximumSize(maximumSize);
            return spec;
        }
    }
}
