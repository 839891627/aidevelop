package com.example.aidevelop.agent.tool;

import com.example.aidevelop.config.AgentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolRouterTest {

    private ToolRouter toolRouter;

    @BeforeEach
    void setUp() {
        AgentProperties agentProperties = new AgentProperties();
        agentProperties.setAllowedTools(List.of("rag.search", "loan.query"));

        AgentTool ragTool = new FixedAgentTool("rag.search", Map.of("source", "rag"));
        AgentTool loanTool = new FixedAgentTool("loan.query", Map.of("source", "loan"));
        AgentTool repaymentTool = new FixedAgentTool("repayment.query", Map.of("source", "repayment"));

        toolRouter = new ToolRouter(List.of(ragTool, loanTool, repaymentTool), agentProperties);
    }

    @Test
    void shouldExecuteAllowedTool() {
        Object result = toolRouter.execute("loan.query", Map.of("userNo", "CUST1001"));

        assertEquals(Map.of("source", "loan"), result);
    }

    @Test
    void shouldRejectToolWhenNotAllowed() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> toolRouter.execute("repayment.query", Map.of()));

        assertTrue(ex.getMessage().contains("工具未授权"));
    }

    @Test
    void shouldExposeAllToolNames() {
        Set<String> names = toolRouter.allToolNames();
        assertEquals(Set.of("rag.search", "loan.query", "repayment.query"), names);
    }

    private static class FixedAgentTool implements AgentTool {
        private final String name;
        private final Object result;

        private FixedAgentTool(String name, Object result) {
            this.name = name;
            this.result = result;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Object execute(Map<String, Object> args) {
            return result;
        }
    }
}
