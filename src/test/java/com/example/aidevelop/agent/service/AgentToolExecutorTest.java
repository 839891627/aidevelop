package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.ToolCall;
import com.example.aidevelop.agent.tool.AgentTool;
import com.example.aidevelop.agent.tool.ToolRouter;
import com.example.aidevelop.config.AgentProperties;
import com.example.aidevelop.config.AgentRateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentToolExecutorTest {

    @Test
    void shouldRetryFailedToolAndReturnSerializedOutput() {
        AgentProperties properties = new AgentProperties();
        properties.setAllowedTools(List.of("loan.query"));
        properties.setToolMaxRetries(1);
        properties.setRetryBackoffMs(0);
        properties.setTimeoutMs(5000);
        ToolRouter router = new ToolRouter(List.of(new FlakyTool()), properties);
        AgentToolExecutor executor = new AgentToolExecutor(router, properties, new ObjectMapper(), AgentRateLimiter.noop());

        AgentToolExecutionResult result = executor.executeWithRetry(
            new ToolCall("loan.query", Map.of("userNo", "USER1001"))
        );

        assertTrue(result.success());
        assertEquals(2, result.attempts());
        assertTrue(result.outputText().contains("\"totalCount\":1"));
    }

    @Test
    void shouldReturnFailedResultWhenAllAttemptsFail() {
        AgentProperties properties = new AgentProperties();
        properties.setAllowedTools(List.of("loan.query"));
        properties.setToolMaxRetries(1);
        properties.setRetryBackoffMs(0);
        properties.setTimeoutMs(5000);
        ToolRouter router = new ToolRouter(List.of(new AlwaysFailTool()), properties);
        AgentToolExecutor executor = new AgentToolExecutor(router, properties, new ObjectMapper(), AgentRateLimiter.noop());

        AgentToolExecutionResult result = executor.executeWithRetry(
            new ToolCall("loan.query", Map.of("userNo", "USER1001"))
        );

        assertFalse(result.success());
        assertEquals(2, result.attempts());
        assertEquals("FAILED", result.outputText());
        assertTrue(result.errorMessage().contains("tool failed"));
    }

    @Test
    void shouldUseToolSpecificTimeoutWhenConfigured() {
        AgentProperties properties = new AgentProperties();
        properties.setAllowedTools(List.of("rag.search"));
        properties.setToolMaxRetries(0);
        properties.setTimeoutMs(10);
        properties.setToolTimeoutMs(Map.of("rag.search", 300));
        ToolRouter router = new ToolRouter(List.of(new SlowRagTool()), properties);
        AgentToolExecutor executor = new AgentToolExecutor(router, properties, new ObjectMapper(), AgentRateLimiter.noop());

        AgentToolExecutionResult result = executor.executeWithRetry(
            new ToolCall("rag.search", Map.of("query", "风险规则"))
        );

        assertTrue(result.success());
        assertTrue(result.outputText().contains("\"documents\":0"));
    }

    @Test
    void shouldReturnRateLimitedResultWhenToolBucketExhausted() {
        AgentProperties properties = new AgentProperties();
        properties.setAllowedTools(List.of("loan.query"));
        properties.setToolMaxRetries(1);
        properties.setRetryBackoffMs(0);
        properties.setTimeoutMs(5000);
        ToolRouter router = new ToolRouter(List.of(new FixedTool()), properties);

        AgentRateLimitProperties rlProps = new AgentRateLimitProperties();
        rlProps.getTool().setDefaultQps(1);        // capacity=1
        rlProps.getTool().setDefaultConcurrency(10);
        rlProps.getTool().setAcquireTimeoutMs(100); // 远小于 1s
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(rlProps);
        AgentToolExecutor executor = new AgentToolExecutor(router, properties, new ObjectMapper(), limiter);

        // 首次调用消费初始令牌，成功
        AgentToolExecutionResult first = executor.executeWithRetry(
            new ToolCall("loan.query", Map.of("userNo", "USER1001"))
        );
        assertTrue(first.success());

        // 第二次令牌不足，100ms 内等不到，返回 RATE_LIMITED 失败（不抛异常、不重试）
        AgentToolExecutionResult second = executor.executeWithRetry(
            new ToolCall("loan.query", Map.of("userNo", "USER1001"))
        );
        assertFalse(second.success());
        assertEquals("RATE_LIMITED", second.outputText());
        assertTrue(second.errorMessage().contains("工具限流"));
    }

    private static class FlakyTool implements AgentTool {
        private int attempts;

        @Override
        public String name() {
            return "loan.query";
        }

        @Override
        public Object execute(Map<String, Object> args) {
            attempts++;
            if (attempts == 1) {
                throw new IllegalStateException("first attempt failed");
            }
            return Map.of("totalCount", 1);
        }
    }

    private static class AlwaysFailTool implements AgentTool {
        @Override
        public String name() {
            return "loan.query";
        }

        @Override
        public Object execute(Map<String, Object> args) {
            throw new IllegalStateException("tool failed");
        }
    }

    private static class SlowRagTool implements AgentTool {
        @Override
        public String name() {
            return "rag.search";
        }

        @Override
        public Object execute(Map<String, Object> args) {
            try {
                Thread.sleep(80);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return Map.of("documents", 0);
        }
    }

    private static class FixedTool implements AgentTool {
        @Override
        public String name() {
            return "loan.query";
        }

        @Override
        public Object execute(Map<String, Object> args) {
            return Map.of("totalCount", 1);
        }
    }
}
