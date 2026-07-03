package com.example.aidevelop.agent.service;

import com.example.aidevelop.config.AgentRateLimitProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 单机令牌桶限流实现。LLM 调用全局共享一桶，工具调用按 toolName 分桶。
 * 每个桶同时控制两个维度：QPS（令牌桶，惰性填充）和并发数（Semaphore）。
 *
 * <p>纯 JDK 实现，无外部依赖。后续可新增 RedisRateLimiter 实现 {@link AgentRateLimiter}
 * 做分布式限流，调用方无感切换。
 *
 * <p>令牌桶参数：填充速率 = 配置的 RPM/60 或 QPS（tokens/sec）；桶容量 = 1 秒填充量（至少 1），
 * 即允许约 1 秒的短时突发，空闲可累积到容量上限。
 */
@Component
@RequiredArgsConstructor
public class TokenBucketRateLimiter implements AgentRateLimiter {

    private static final String LLM_KEY = "llm:global";

    private final AgentRateLimitProperties properties;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** 始终放行的许可，close 空实现，限流禁用时复用。 */
    private static final Permit NOOP_PERMIT = () -> {
    };

    @Override
    public Optional<Permit> acquireLlm(String phase) {
        if (!properties.isEnabled() || !properties.getLlm().isEnabled()) {
            return Optional.of(NOOP_PERMIT);
        }
        double tokensPerSecond = Math.max(0.0, properties.getLlm().getRpm() / 60.0);
        Bucket bucket = buckets.computeIfAbsent(LLM_KEY,
            k -> new Bucket(tokensPerSecond, properties.getLlm().getConcurrency()));
        return bucket.tryAcquire(1, properties.getLlm().getAcquireTimeoutMs());
    }

    @Override
    public Optional<Permit> acquireTool(String toolName) {
        if (!properties.isEnabled() || !properties.getTool().isEnabled()) {
            return Optional.of(NOOP_PERMIT);
        }
        AgentRateLimitProperties.Tool toolCfg = properties.getTool();
        AgentRateLimitProperties.ToolLimit perTool = toolCfg.getPerTool().get(toolName);
        double qps = (perTool != null && perTool.getQps() > 0) ? perTool.getQps() : toolCfg.getDefaultQps();
        int concurrency = (perTool != null && perTool.getConcurrency() > 0)
            ? perTool.getConcurrency() : toolCfg.getDefaultConcurrency();
        Bucket bucket = buckets.computeIfAbsent("tool:" + toolName,
            k -> new Bucket(qps, concurrency));
        return bucket.tryAcquire(1, toolCfg.getAcquireTimeoutMs());
    }

    /**
     * 单个限流桶：QPS 令牌桶（惰性填充）+ 并发 Semaphore。
     */
    private static final class Bucket {
        private final double tokensPerNano;
        private final int capacity;
        private final Semaphore concurrency;
        private double availableTokens;
        private long lastRefillNanos;
        private final Object lock = new Object();

        Bucket(double tokensPerSecond, int concurrency) {
            this.tokensPerNano = tokensPerSecond / 1_000_000_000.0;
            this.capacity = Math.max(1, (int) Math.ceil(tokensPerSecond));
            this.concurrency = new Semaphore(Math.max(1, concurrency), true);
            this.availableTokens = this.capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        Optional<Permit> tryAcquire(int permits, long timeoutMs) {
            long deadlineNanos = System.nanoTime() + Math.max(0, TimeUnit.MILLISECONDS.toNanos(timeoutMs));

            // 1. 先抢并发许可，带超时
            long concurrencyWaitNanos = deadlineNanos - System.nanoTime();
            if (concurrencyWaitNanos <= 0) {
                return Optional.empty();
            }
            boolean gotConcurrency;
            try {
                gotConcurrency = concurrency.tryAcquire(permits, concurrencyWaitNanos, TimeUnit.NANOSECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
            if (!gotConcurrency) {
                return Optional.empty();
            }

            // 2. 再抢 QPS 令牌，不够则 sleep 等填充，超时归还并发许可
            boolean acquired = false;
            try {
                while (true) {
                    long nowNanos = System.nanoTime();
                    long remainingNanos = deadlineNanos - nowNanos;
                    if (remainingNanos <= 0) {
                        return Optional.empty();
                    }
                    long waitNanos;
                    synchronized (lock) {
                        refill(nowNanos);
                        if (availableTokens >= permits) {
                            availableTokens -= permits;
                            acquired = true;
                            return Optional.of(new SemaphorePermit(concurrency, permits));
                        }
                        double need = permits - availableTokens;
                        waitNanos = tokensPerNano > 0
                            ? (long) Math.ceil(need / tokensPerNano)
                            : remainingNanos;
                    }
                    waitNanos = Math.min(waitNanos, remainingNanos);
                    if (waitNanos <= 0) {
                        waitNanos = 1;
                    }
                    try {
                        long ms = waitNanos / 1_000_000;
                        int ns = (int) (waitNanos % 1_000_000);
                        Thread.sleep(ms, ns);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return Optional.empty();
                    }
                }
            } finally {
                // 失败路径（超时/中断）归还并发许可；成功路径由 Permit.close 归还
                if (!acquired) {
                    concurrency.release(permits);
                }
            }
        }

        private void refill(long nowNanos) {
            if (nowNanos <= lastRefillNanos) {
                return;
            }
            long elapsed = nowNanos - lastRefillNanos;
            availableTokens = Math.min(capacity, availableTokens + elapsed * tokensPerNano);
            lastRefillNanos = nowNanos;
        }
    }

    /** 归还并发许可的 Permit 实现，防重复 release。 */
    private static final class SemaphorePermit implements Permit {
        private final Semaphore semaphore;
        private final int permits;
        private volatile boolean released;

        SemaphorePermit(Semaphore semaphore, int permits) {
            this.semaphore = semaphore;
            this.permits = permits;
        }

        @Override
        public void close() {
            if (!released) {
                released = true;
                semaphore.release(permits);
            }
        }
    }
}
