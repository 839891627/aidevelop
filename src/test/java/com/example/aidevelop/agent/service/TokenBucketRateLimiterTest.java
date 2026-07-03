package com.example.aidevelop.agent.service;

import com.example.aidevelop.config.AgentRateLimitProperties;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBucketRateLimiterTest {

    @Test
    void shouldAcquireImmediatelyWhenBucketHasTokens() {
        AgentRateLimitProperties props = newProps();
        props.getLlm().setRpm(60);        // 1 token/sec, capacity=1
        props.getLlm().setConcurrency(5);
        props.getLlm().setAcquireTimeoutMs(1000);
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(props);

        Optional<AgentRateLimiter.Permit> permit = limiter.acquireLlm("PLAN");

        assertThat(permit).isPresent();
        permit.get().close();
    }

    @Test
    void shouldReturnEmptyWhenConcurrencyExhaustedAndTimeout() {
        AgentRateLimitProperties props = newProps();
        props.getTool().setDefaultQps(100);   // QPS 充足，排除 QPS 限流干扰
        props.getTool().setDefaultConcurrency(1);
        props.getTool().setAcquireTimeoutMs(200);
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(props);

        Optional<AgentRateLimiter.Permit> first = limiter.acquireTool("loan.query");
        assertThat(first).isPresent();
        // 并发许可已被占满（=1），第二次在 200ms 内拿不到
        Optional<AgentRateLimiter.Permit> second = limiter.acquireTool("loan.query");
        assertThat(second).isEmpty();
        first.get().close();
    }

    @Test
    void shouldReleaseConcurrencyOnClose() {
        AgentRateLimitProperties props = newProps();
        props.getTool().setDefaultQps(100);
        props.getTool().setDefaultConcurrency(1);
        props.getTool().setAcquireTimeoutMs(1000);
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(props);

        Optional<AgentRateLimiter.Permit> first = limiter.acquireTool("loan.query");
        first.get().close();   // 归还并发许可
        Optional<AgentRateLimiter.Permit> second = limiter.acquireTool("loan.query");
        assertThat(second).isPresent();
        second.get().close();
    }

    @Test
    void shouldReturnEmptyWhenQpsTokenNotEnoughWithinTimeout() {
        AgentRateLimitProperties props = newProps();
        props.getTool().setDefaultQps(1);       // 1 token/sec, capacity=1
        props.getTool().setDefaultConcurrency(10);
        props.getTool().setAcquireTimeoutMs(200);   // 远小于 1s
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(props);

        Optional<AgentRateLimiter.Permit> first = limiter.acquireTool("rag.search");
        assertThat(first).isPresent();
        first.get().close();   // 归还并发，但 QPS 令牌已消费，需 1s 补充
        Optional<AgentRateLimiter.Permit> second = limiter.acquireTool("rag.search");
        assertThat(second).isEmpty();
    }

    @Test
    void shouldIsolateBucketsByToolName() {
        AgentRateLimitProperties props = newProps();
        props.getTool().setDefaultQps(1);
        props.getTool().setDefaultConcurrency(1);
        props.getTool().setAcquireTimeoutMs(200);
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(props);

        // loan.query 占满并发，不影响 rag.search 的独立桶
        Optional<AgentRateLimiter.Permit> loan = limiter.acquireTool("loan.query");
        assertThat(loan).isPresent();
        Optional<AgentRateLimiter.Permit> rag = limiter.acquireTool("rag.search");
        assertThat(rag).isPresent();
        loan.get().close();
        rag.get().close();
    }

    @Test
    void shouldBypassWhenDisabled() {
        AgentRateLimitProperties props = newProps();
        props.setEnabled(false);
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(props);

        Optional<AgentRateLimiter.Permit> permit = limiter.acquireLlm("PLAN");
        assertThat(permit).isPresent();
        permit.get().close();
    }

    private AgentRateLimitProperties newProps() {
        AgentRateLimitProperties props = new AgentRateLimitProperties();
        props.setEnabled(true);
        props.getLlm().setEnabled(true);
        props.getTool().setEnabled(true);
        return props;
    }
}
