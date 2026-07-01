package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.ToolCall;
import com.example.aidevelop.agent.tool.AgentTool;
import com.example.aidevelop.agent.tool.ToolRouter;
import com.example.aidevelop.config.AgentProperties;
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
        AgentToolExecutor executor = new AgentToolExecutor(router, properties, new ObjectMapper());

        AgentToolExecutionResult result = executor.executeWithRetry(
            new ToolCall("loan.query", Map.of("userNo", "CUST1001"))
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
        AgentToolExecutor executor = new AgentToolExecutor(router, properties, new ObjectMapper());

        AgentToolExecutionResult result = executor.executeWithRetry(
            new ToolCall("loan.query", Map.of("userNo", "CUST1001"))
        );

        assertFalse(result.success());
        assertEquals(2, result.attempts());
        assertEquals("FAILED", result.outputText());
        assertTrue(result.errorMessage().contains("tool failed"));
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
}
