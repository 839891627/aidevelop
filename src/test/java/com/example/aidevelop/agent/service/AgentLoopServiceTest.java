package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.agent.model.AgentResponse;
import com.example.aidevelop.agent.model.AgentStep;
import com.example.aidevelop.agent.tool.AgentTool;
import com.example.aidevelop.agent.tool.ToolRouter;
import com.example.aidevelop.config.AgentProperties;
import com.example.aidevelop.config.RouteProperties;
import com.example.aidevelop.config.ToolsProperties;
import com.example.aidevelop.service.IntentRoutingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentLoopServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private AgentLoopService agentLoopService;
    private ToolRouter toolRouter;

    @BeforeEach
    void setUp() {
        AgentProperties agentProperties = new AgentProperties();
        agentProperties.setEnabled(true);
        agentProperties.setMaxSteps(3);
        agentProperties.setTimeoutMs(5000);
        agentProperties.setToolMaxRetries(1);
        agentProperties.setRetryBackoffMs(0);
        agentProperties.setReflectEnabled(true);
        agentProperties.setReplanEnabled(true);
        agentProperties.setMaxReplanRounds(1);
        agentProperties.setSelfCheckEnabled(true);
        agentProperties.setSelfCheckMinScore(70);
        agentProperties.setMinObservationCount(1);
        agentProperties.setFallbackOnReplanFailure(true);
        agentProperties.setFallbackWhenSelfCheckFailed(true);
        agentProperties.setAllowedTools(List.of("loan.query", "rag.search", "repayment.query", "risk.assess"));

        ToolsProperties toolsProperties = new ToolsProperties();
        toolsProperties.setEnabled(List.of("loanQueryFunction", "repaymentQueryFunction", "riskAssessmentFunction"));
        IntentRoutingService intentRoutingService = new IntentRoutingService(new RouteProperties(), toolsProperties);

        toolRouter = new ToolRouter(List.of(
            new FixedAgentTool("loan.query", Map.of("totalCount", 1)),
            new FixedAgentTool("repayment.query", Map.of("totalCount", 0)),
            new FixedAgentTool("rag.search", Map.of("documents", List.of("risk_policy_doc"))),
            new FixedAgentTool("risk.assess", Map.of("riskLevel", "LOW"))
        ), agentProperties);

        agentLoopService = new AgentLoopService(intentRoutingService, toolRouter, new ObjectMapper(), agentProperties);
        ReflectionTestUtils.setField(agentLoopService, "chatClient", chatClient);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
    }

    @Test
    void shouldExecutePlanToolRespondFlow() {
        when(callResponseSpec.content())
            .thenReturn("{\"toolCalls\":[{\"toolName\":\"loan.query\",\"args\":{\"userNo\":\"CUST1001\"}}],\"done\":false}")
            .thenReturn("{\"done\":true,\"reason\":\"已获得借款信息\"}")
            .thenReturn("这是最终回答")
            .thenReturn("{\"pass\":true,\"score\":90,\"reason\":\"证据充分\"}");

        AgentRequest request = new AgentRequest();
        request.setMessage("请查询 CUST1001 的借款记录");
        request.setMaxSteps(3);

        AgentResponse response = agentLoopService.chat(request);

        assertNotNull(response.getTraceId());
        assertEquals("TOOL_ONLY", response.getRouteType());
        assertEquals("这是最终回答", response.getFinalAnswer());
        assertFalse(response.getSteps().isEmpty());
        assertTrue(response.getSteps().stream().anyMatch(step -> step.getActionType().name().equals("REFLECT")));
    }

    @Test
    void shouldRetryToolWhenFirstAttemptFails() {
        AgentProperties retryProperties = new AgentProperties();
        retryProperties.setEnabled(true);
        retryProperties.setMaxSteps(3);
        retryProperties.setTimeoutMs(5000);
        retryProperties.setToolMaxRetries(1);
        retryProperties.setRetryBackoffMs(0);
        retryProperties.setReflectEnabled(true);
        retryProperties.setReplanEnabled(true);
        retryProperties.setMaxReplanRounds(1);
        retryProperties.setSelfCheckEnabled(true);
        retryProperties.setSelfCheckMinScore(70);
        retryProperties.setMinObservationCount(1);
        retryProperties.setFallbackOnReplanFailure(true);
        retryProperties.setFallbackWhenSelfCheckFailed(true);
        retryProperties.setAllowedTools(List.of("loan.query"));

        ToolsProperties toolsProperties = new ToolsProperties();
        toolsProperties.setEnabled(List.of("loanQueryFunction"));
        IntentRoutingService intentRoutingService = new IntentRoutingService(new RouteProperties(), toolsProperties);

        ToolRouter flakyRouter = new ToolRouter(List.of(new FlakyAgentTool()), retryProperties);
        AgentLoopService retryService = new AgentLoopService(intentRoutingService, flakyRouter, new ObjectMapper(), retryProperties);
        ReflectionTestUtils.setField(retryService, "chatClient", chatClient);

        when(callResponseSpec.content())
            .thenReturn("{\"toolCalls\":[{\"toolName\":\"loan.query\",\"args\":{\"userNo\":\"CUST1001\"}}],\"done\":false}")
            .thenReturn("{\"done\":true,\"reason\":\"重试后已成功\"}")
            .thenReturn("重试成功后的回答")
            .thenReturn("{\"pass\":true,\"score\":88,\"reason\":\"回答可靠\"}");

        AgentRequest request = new AgentRequest();
        request.setMessage("请查询 CUST1001 的借款记录");

        AgentResponse response = retryService.chat(request);
        AgentStep toolStep = response.getSteps().stream()
            .filter(step -> step.getActionType().name().equals("TOOL"))
            .findFirst()
            .orElseThrow();
        assertTrue(toolStep.getToolOutput().contains("attempts=2"));
    }

    @Test
    void shouldReplanWhenReflectSaysNotDone() {
        when(callResponseSpec.content())
            .thenReturn("{\"toolCalls\":[{\"toolName\":\"loan.query\",\"args\":{\"userNo\":\"CUST1001\"}}],\"done\":false}")
            .thenReturn("{\"done\":false,\"reason\":\"还需要还款信息\"}")
            .thenReturn("{\"toolCalls\":[{\"toolName\":\"repayment.query\",\"args\":{\"userNo\":\"CUST1001\"}}],\"done\":false}")
            .thenReturn("{\"done\":true,\"reason\":\"信息足够\"}")
            .thenReturn("已完成二次规划后的回答")
            .thenReturn("{\"pass\":true,\"score\":92,\"reason\":\"证据充足\"}");

        AgentRequest request = new AgentRequest();
        request.setMessage("请分析 CUST1001 的借款和还款信息");
        request.setMaxSteps(3);

        AgentResponse response = agentLoopService.chat(request);

        long planCount = response.getSteps().stream()
            .filter(step -> step.getActionType().name().equals("PLAN"))
            .count();
        boolean hasRepaymentTool = response.getSteps().stream()
            .anyMatch(step -> "repayment.query".equals(step.getToolName()));

        assertTrue(planCount >= 2);
        assertTrue(hasRepaymentTool);
        assertEquals("已完成二次规划后的回答", response.getFinalAnswer());
    }

    @Test
    void shouldFallbackWhenReplanFails() {
        when(callResponseSpec.content())
            .thenReturn("{\"toolCalls\":[{\"toolName\":\"loan.query\",\"args\":{\"userNo\":\"CUST1001\"}}],\"done\":false}")
            .thenReturn("{\"done\":false,\"reason\":\"仍需更多信息\"}")
            .thenThrow(new RuntimeException("replan llm error"))
            .thenReturn("这是一条草稿回答")
            .thenReturn("{\"pass\":true,\"score\":95,\"reason\":\"形式上通过\"}");

        AgentRequest request = new AgentRequest();
        request.setMessage("请综合分析 CUST1001 的借款情况");

        AgentResponse response = agentLoopService.chat(request);

        assertTrue(response.getFinalAnswer().contains("基于当前可验证信息，我先给出稳健结论"));
        assertTrue(response.getSteps().stream().anyMatch(step -> step.getActionType().name().equals("SELF_CHECK")));
    }

    @Test
    void shouldForceRagEvidenceForRiskIntent() {
        when(callResponseSpec.content())
            .thenReturn("{\"toolCalls\":[{\"toolName\":\"loan.query\",\"args\":{\"userNo\":\"CUST1001\"}}],\"done\":false}")
            .thenReturn("{\"done\":false,\"reason\":\"仍需知识库证据\"}")
            .thenReturn("{\"done\":true,\"reason\":\"信息已充分\"}")
            .thenReturn("最终风险回答")
            .thenReturn("{\"pass\":true,\"score\":90,\"reason\":\"证据充分\"}");

        AgentRequest request = new AgentRequest();
        request.setMessage("请对 CUST1001 做风险评估并给出风险判断");
        request.setMaxSteps(3);

        AgentResponse response = agentLoopService.chat(request);

        boolean hasRagTool = response.getSteps().stream().anyMatch(step -> "rag.search".equals(step.getToolName()));
        boolean hasRiskTool = response.getSteps().stream().anyMatch(step -> "risk.assess".equals(step.getToolName()));
        assertTrue(hasRagTool);
        assertTrue(hasRiskTool);
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

    private static class FlakyAgentTool implements AgentTool {
        private int attempts = 0;

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
}
