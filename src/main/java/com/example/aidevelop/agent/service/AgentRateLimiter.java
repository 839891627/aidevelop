package com.example.aidevelop.agent.service;

import java.util.Optional;

/**
 * Agent L3 资源限流器抽象。对 LLM 调用和工具调用分别限流，
 * 默认单机令牌桶实现见 {@link TokenBucketRateLimiter}，后续可扩展 Redis 分布式实现。
 *
 * <p>调用方应使用 try-with-resources 持有 {@link Permit}，块结束时归还并发许可：
 * <pre>{@code
 * try (AgentRateLimiter.Permit permit = rateLimiter.acquireLlm("PLAN")
 *         .orElseThrow(() -> new AgentRateLimitedException("PLAN"))) {
 *     // 实际 LLM/工具调用
 * }
 * }</pre>
 */
public interface AgentRateLimiter {

    /**
     * 为一次 LLM 阶段调用获取限流许可。全局共享一个桶（key=llm:global）。
     *
     * @param phase LLM 阶段名，如 PLAN/REFLECT/RESPOND，仅用于异常信息
     * @return 获取成功返回 Permit，超时返回 empty
     */
    Optional<Permit> acquireLlm(String phase);

    /**
     * 为一次工具调用获取限流许可。按 toolName 分桶。
     *
     * @param toolName 工具名，如 rag.search/loan.query
     * @return 获取成功返回 Permit，超时返回 empty
     */
    Optional<Permit> acquireTool(String toolName);

    /**
     * 不限流的实现，供测试和限流禁用场景使用：始终放行，close 空实现。
     */
    static AgentRateLimiter noop() {
        return new AgentRateLimiter() {
            @Override
            public Optional<Permit> acquireLlm(String phase) {
                return Optional.of(() -> {
                });
            }

            @Override
            public Optional<Permit> acquireTool(String toolName) {
                return Optional.of(() -> {
                });
            }
        };
    }

    /**
     * 限流许可，try-with-resources 结束时归还并发许可。QPS 令牌不归还（已消费）。
     *
     * <p>覆盖 AutoCloseable.close() 收窄为不抛 checked exception，使本接口成为函数接口，
     * 支持 {@code () -> {}} 形式的 lambda 实现（用于 noop 放行许可）。
     */
    interface Permit extends AutoCloseable {
        @Override
        void close();
    }
}
