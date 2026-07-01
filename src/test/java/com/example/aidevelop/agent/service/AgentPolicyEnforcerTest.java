package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.agent.model.ToolCall;
import com.example.aidevelop.agent.tool.AgentTool;
import com.example.aidevelop.agent.tool.ToolRouter;
import com.example.aidevelop.config.AgentProperties;
import com.example.aidevelop.service.IntentRoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentPolicyEnforcerTest {

    private AgentPolicyEnforcer policyEnforcer;

    @BeforeEach
    void setUp() {
        AgentProperties properties = new AgentProperties();
        properties.setAllowedTools(List.of("rag.search", "loan.query", "risk.assess"));
        properties.setForceRagForRiskEvaluation(true);
        ToolRouter router = new ToolRouter(List.of(
            new FixedTool("rag.search"),
            new FixedTool("loan.query"),
            new FixedTool("risk.assess")
        ), properties);
        policyEnforcer = new AgentPolicyEnforcer(properties, router);
    }

    @Test
    void shouldResolveLegacyRouteToolsAndForceRagForRiskIntent() {
        IntentRoutingService.RoutePlan routePlan = new IntentRoutingService.RoutePlan(
            IntentRoutingService.RouteType.TOOL_ONLY,
            false,
            List.of("loanQueryFunction", "riskAssessmentFunction"),
            3,
            0.2,
            3,
            30000,
            "test"
        );

        List<String> allowedTools = policyEnforcer.resolveAllowedTools(routePlan, "请判断 CUST1001 风险");

        assertEquals(List.of("loan.query", "risk.assess", "rag.search"), allowedTools);
    }

    @Test
    void shouldPrependRequiredRiskToolsBeforePlannedCalls() {
        AgentRequest request = new AgentRequest();
        request.setMessage("请对 CUST1001 做风险评估");
        request.setConversationId("conv-1");
        IntentRoutingService.RoutePlan routePlan = new IntentRoutingService.RoutePlan(
            IntentRoutingService.RouteType.HYBRID,
            true,
            List.of("riskAssessmentFunction"),
            3,
            0.2,
            3,
            30000,
            "test"
        );

        List<ToolCall> enforced = policyEnforcer.enforcePolicyTools(
            request,
            routePlan,
            List.of("rag.search", "risk.assess"),
            List.of(new ToolCall("risk.assess", Map.of("userNo", "CUST1001"))),
            List.of(),
            3
        );

        assertEquals("rag.search", enforced.get(0).toolName());
        assertEquals("risk.assess", enforced.get(1).toolName());
        assertTrue(enforced.get(0).args().containsKey("conversationId"));
    }

    private static class FixedTool implements AgentTool {
        private final String name;

        private FixedTool(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Object execute(Map<String, Object> args) {
            return Map.of();
        }
    }
}
