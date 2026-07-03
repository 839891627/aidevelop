package com.example.aidevelop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent L3 资源限流配置。前缀 app.chat.agent.rate-limit。
 *
 * <p>分两层：LLM 全局限流（保护 LLM provider 的 RPM/并发）和工具按 toolName 限流（保护各工具后端）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.chat.agent.rate-limit")
public class AgentRateLimitProperties {

    /**
     * L3 限流总开关。
     */
    private boolean enabled = true;

    private Llm llm = new Llm();

    private Tool tool = new Tool();

    @Data
    public static class Llm {
        private boolean enabled = true;

        /**
         * 全局 LLM 每分钟最大调用数（RPM）。
         */
        private int rpm = 60;

        /**
         * 全局 LLM 最大并发调用数。
         */
        private int concurrency = 10;

        /**
         * 获取令牌最大等待时间（毫秒），超时抛 AgentRateLimitedException 走降级。
         */
        private long acquireTimeoutMs = 5000;
    }

    @Data
    public static class Tool {
        private boolean enabled = true;

        /**
         * 单个工具默认每秒最大调用数（QPS）。
         */
        private double defaultQps = 5;

        /**
         * 单个工具默认最大并发数。
         */
        private int defaultConcurrency = 3;

        /**
         * 获取令牌最大等待时间（毫秒），超时返回失败 result 走降级。
         */
        private long acquireTimeoutMs = 2000;

        /**
         * 按工具名覆盖 QPS/并发配置，未命中的工具用 default*。
         */
        private Map<String, ToolLimit> perTool = new LinkedHashMap<>();
    }

    @Data
    public static class ToolLimit {
        private double qps;
        private int concurrency;
    }
}
