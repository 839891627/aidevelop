package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.agent.model.AgentResponse;
import com.example.aidevelop.agent.model.AgentStep;
import com.example.aidevelop.agent.tool.AgentTool;
import com.example.aidevelop.agent.tool.ToolRouter;
import com.example.aidevelop.config.AgentProperties;
import com.example.aidevelop.config.AgentRateLimitProperties;
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

    private AgentTraceService agentTraceService;

    private AgentLoopService agentLoopService;
    private ToolRouter toolRouter;

    @BeforeEach
    void setUp() {
        AgentProperties agentProperties = new AgentProperties();
        agentTraceService = new NoopAgentTraceService();
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

        AgentPolicyEnforcer policyEnforcer = new AgentPolicyEnforcer(agentProperties, toolRouter);
        AgentToolExecutor toolExecutor = new AgentToolExecutor(toolRouter, agentProperties, new ObjectMapper(), AgentRateLimiter.noop());
        AgentLlmClient agentLlmClient = new AgentLlmClient(agentProperties, AgentRateLimiter.noop());
        ReflectionTestUtils.setField(agentLlmClient, "chatClient", chatClient);
        AgentStructuredOutputValidator validator = new AgentStructuredOutputValidator(new ObjectMapper());
        AgentPlanner planner = new AgentPlanner(policyEnforcer, toolRouter, agentLlmClient, validator);
        AgentReflector reflector = new AgentReflector(agentLlmClient, validator);
        AgentResponder responder = new AgentResponder(agentProperties, policyEnforcer, agentLlmClient, validator);
        agentLoopService = new AgentLoopService(
            intentRoutingService,
            agentProperties,
            policyEnforcer,
            toolExecutor,
            planner,
            reflector,
            responder,
            agentTraceService
        );
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
    }

    @Test
    void shouldExecutePlanToolRespondFlow() {
        when(callResponseSpec.content())
            .thenReturn("{\"toolCalls\":[{\"toolName\":\"loan.query\",\"args\":{\"userNo\":\"USER1001\"}}],\"done\":false}")
            .thenReturn("{\"done\":true,\"reason\":\"已获得借款信息\"}")
            .thenReturn("这是最终回答")
            .thenReturn("{\"pass\":true,\"score\":90,\"reason\":\"证据充分\"}");

        AgentRequest request = new AgentRequest();
        request.setMessage("请查询 USER1001 的借款记录");
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
        AgentPolicyEnforcer retryPolicyEnforcer = new AgentPolicyEnforcer(retryProperties, flakyRouter);
        AgentToolExecutor retryToolExecutor = new AgentToolExecutor(flakyRouter, retryProperties, new ObjectMapper(), AgentRateLimiter.noop());
        AgentLlmClient retryLlmClient = new AgentLlmClient(retryProperties, AgentRateLimiter.noop());
        ReflectionTestUtils.setField(retryLlmClient, "chatClient", chatClient);
        AgentStructuredOutputValidator retryValidator = new AgentStructuredOutputValidator(new ObjectMapper());
        AgentPlanner retryPlanner = new AgentPlanner(retryPolicyEnforcer, flakyRouter, retryLlmClient, retryValidator);
        AgentReflector retryReflector = new AgentReflector(retryLlmClient, retryValidator);
        AgentResponder retryResponder = new AgentResponder(retryProperties, retryPolicyEnforcer, retryLlmClient, retryValidator);
        AgentLoopService retryService = new AgentLoopService(
            intentRoutingService,
            retryProperties,
            retryPolicyEnforcer,
            retryToolExecutor,
            retryPlanner,
            retryReflector,
            retryResponder,
            agentTraceService
        );
        when(callResponseSpec.content())
            .thenReturn("{\"toolCalls\":[{\"toolName\":\"loan.query\",\"args\":{\"userNo\":\"USER1001\"}}],\"done\":false}")
            .thenReturn("{\"done\":true,\"reason\":\"重试后已成功\"}")
            .thenReturn("重试成功后的回答")
            .thenReturn("{\"pass\":true,\"score\":88,\"reason\":\"回答可靠\"}");

        AgentRequest request = new AgentRequest();
        request.setMessage("请查询 USER1001 的借款记录");

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
            .thenReturn("{\"toolCalls\":[{\"toolName\":\"loan.query\",\"args\":{\"userNo\":\"USER1001\"}}],\"done\":false}")
            .thenReturn("{\"done\":false,\"reason\":\"还需要还款信息\"}")
            .thenReturn("{\"toolCalls\":[{\"toolName\":\"repayment.query\",\"args\":{\"userNo\":\"USER1001\"}}],\"done\":false}")
            .thenReturn("{\"done\":true,\"reason\":\"信息足够\"}")
            .thenReturn("已完成二次规划后的回答")
            .thenReturn("{\"pass\":true,\"score\":92,\"reason\":\"证据充足\"}");

        AgentRequest request = new AgentRequest();
        request.setMessage("请分析 USER1001 的借款和还款信息");
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
            .thenReturn("{\"toolCalls\":[{\"toolName\":\"loan.query\",\"args\":{\"userNo\":\"USER1001\"}}],\"done\":false}")
            .thenReturn("{\"done\":false,\"reason\":\"仍需更多信息\"}")
            .thenThrow(new RuntimeException("replan llm error"))
            .thenReturn("这是一条草稿回答")
            .thenReturn("{\"pass\":true,\"score\":95,\"reason\":\"形式上通过\"}");

        AgentRequest request = new AgentRequest();
        request.setMessage("请综合分析 USER1001 的借款情况");

        AgentResponse response = agentLoopService.chat(request);

        assertTrue(response.getFinalAnswer().contains("基于当前可验证信息，我先给出稳健结论"));
        assertTrue(response.getSteps().stream().anyMatch(step -> step.getActionType().name().equals("SELF_CHECK")));
    }

    @Test
    void shouldForceRagEvidenceForRiskIntent() {
        when(callResponseSpec.content())
            .thenReturn("{\"toolCalls\":[{\"toolName\":\"loan.query\",\"args\":{\"userNo\":\"USER1001\"}}],\"done\":false}")
            .thenReturn("{\"done\":false,\"reason\":\"仍需知识库证据\"}")
            .thenReturn("{\"done\":true,\"reason\":\"信息已充分\"}")
            .thenReturn("最终风险回答")
            .thenReturn("{\"pass\":true,\"score\":90,\"reason\":\"证据充分\"}");

        AgentRequest request = new AgentRequest();
        request.setMessage("请对 USER1001 做风险评估并给出风险判断");
        request.setMaxSteps(3);

        AgentResponse response = agentLoopService.chat(request);

        boolean hasRagTool = response.getSteps().stream().anyMatch(step -> "rag.search".equals(step.getToolName()));
        boolean hasRiskTool = response.getSteps().stream().anyMatch(step -> "risk.assess".equals(step.getToolName()));
        assertTrue(hasRagTool);
        assertTrue(hasRiskTool);
    }

    @Test
    void shouldSupplementRagBeforeFirstSelfCheckWhenRiskEvidenceIsMissing() {
        when(callResponseSpec.content())
            .thenReturn("""
                {"toolCalls":[
                  {"toolName":"loan.query","args":{"userNo":"USER1001"}},
                  {"toolName":"repayment.query","args":{"userNo":"USER1001"}}
                ],"done":false}
                """)
            .thenReturn("{\"done\":true,\"reason\":\"业务信息已足够，但风险问题仍需知识库证据\"}")
            .thenReturn("补充 RAG 后的最终风险回答")
            .thenReturn("{\"pass\":true,\"score\":95,\"reason\":\"证据充分\"}");

        AgentRequest request = new AgentRequest();
        request.setMessage("请对 USER1001 做风险评估并给出风险判断");
        request.setMaxSteps(3);

        AgentResponse response = agentLoopService.chat(request);

        List<String> toolNames = response.getSteps().stream()
            .filter(step -> step.getToolName() != null)
            .map(AgentStep::getToolName)
            .toList();

        assertEquals(List.of("risk.assess", "loan.query", "repayment.query", "rag.search"), toolNames);
        assertEquals("补充 RAG 后的最终风险回答", response.getFinalAnswer());
        assertTrue(response.getBudgetSummary().getToolCalls() >= 4);

        List<String> actionTypes = response.getSteps().stream()
            .map(step -> step.getActionType().name())
            .toList();
        int ragStepIndex = response.getSteps().stream()
            .filter(step -> "rag.search".equals(step.getToolName()))
            .findFirst()
            .orElseThrow()
            .getStepIndex();
        int firstSelfCheckIndex = response.getSteps().stream()
            .filter(step -> step.getActionType().name().equals("SELF_CHECK"))
            .findFirst()
            .orElseThrow()
            .getStepIndex();

        assertTrue(ragStepIndex < firstSelfCheckIndex);
        assertEquals(1, actionTypes.stream().filter("SELF_CHECK"::equals).count());
    }

    @Test
    void shouldReserveFinalAnswerLlmCallsOutsidePlanningRoundBudget() {
        AgentProperties limitedProperties = new AgentProperties();
        limitedProperties.setEnabled(true);
        limitedProperties.setMaxSteps(3);
        limitedProperties.setMaxLlmCallsPerRound(2);
        limitedProperties.setTimeoutMs(5000);
        limitedProperties.setToolMaxRetries(1);
        limitedProperties.setRetryBackoffMs(0);
        limitedProperties.setReflectEnabled(true);
        limitedProperties.setReplanEnabled(true);
        limitedProperties.setMaxReplanRounds(1);
        limitedProperties.setSelfCheckEnabled(true);
        limitedProperties.setSelfCheckMinScore(70);
        limitedProperties.setMinObservationCount(1);
        limitedProperties.setFallbackOnReplanFailure(true);
        limitedProperties.setFallbackWhenSelfCheckFailed(true);
        limitedProperties.setAllowedTools(List.of("loan.query"));

        ToolsProperties toolsProperties = new ToolsProperties();
        toolsProperties.setEnabled(List.of("loanQueryFunction"));
        IntentRoutingService intentRoutingService = new IntentRoutingService(new RouteProperties(), toolsProperties);

        ToolRouter limitedRouter = new ToolRouter(
            List.of(new FixedAgentTool("loan.query", Map.of("totalCount", 1))),
            limitedProperties
        );
        AgentPolicyEnforcer limitedPolicyEnforcer = new AgentPolicyEnforcer(limitedProperties, limitedRouter);
        AgentToolExecutor limitedToolExecutor = new AgentToolExecutor(limitedRouter, limitedProperties, new ObjectMapper(), AgentRateLimiter.noop());
        AgentLlmClient limitedLlmClient = new AgentLlmClient(limitedProperties, AgentRateLimiter.noop());
        ReflectionTestUtils.setField(limitedLlmClient, "chatClient", chatClient);
        AgentStructuredOutputValidator limitedValidator = new AgentStructuredOutputValidator(new ObjectMapper());
        AgentLoopService limitedService = new AgentLoopService(
            intentRoutingService,
            limitedProperties,
            limitedPolicyEnforcer,
            limitedToolExecutor,
            new AgentPlanner(limitedPolicyEnforcer, limitedRouter, limitedLlmClient, limitedValidator),
            new AgentReflector(limitedLlmClient, limitedValidator),
            new AgentResponder(limitedProperties, limitedPolicyEnforcer, limitedLlmClient, limitedValidator),
            agentTraceService
        );

        when(callResponseSpec.content())
            .thenReturn("{\"toolCalls\":[{\"toolName\":\"loan.query\",\"args\":{\"userNo\":\"USER1001\"}}],\"done\":false}")
            .thenReturn("{\"done\":true,\"reason\":\"已获得借款信息\"}")
            .thenReturn("最终回答未被规划轮次预算拦截")
            .thenReturn("{\"pass\":true,\"score\":90,\"reason\":\"证据充分\"}");

        AgentRequest request = new AgentRequest();
        request.setMessage("请查询 USER1001 的借款记录");
        request.setMaxSteps(3);

        AgentResponse response = limitedService.chat(request);

        assertEquals("最终回答未被规划轮次预算拦截", response.getFinalAnswer());
        assertTrue(response.isCompleted());
        assertTrue(response.getSteps().stream().anyMatch(step -> step.getActionType().name().equals("SELF_CHECK")));
    }

    @Test
    void shouldFallbackWhenLlmRateLimited() {
        AgentProperties properties = new AgentProperties();
        properties.setEnabled(true);
        properties.setMaxSteps(3);
        properties.setTimeoutMs(5000);
        properties.setToolMaxRetries(0);
        properties.setRetryBackoffMs(0);
        properties.setReflectEnabled(true);
        properties.setReplanEnabled(false);
        properties.setSelfCheckEnabled(true);
        properties.setSelfCheckMinScore(70);
        properties.setMinObservationCount(1);
        properties.setFallbackWhenSelfCheckFailed(true);
        properties.setAllowedTools(List.of("loan.query"));

        ToolsProperties toolsProperties = new ToolsProperties();
        toolsProperties.setEnabled(List.of("loanQueryFunction"));
        IntentRoutingService intentRoutingService = new IntentRoutingService(new RouteProperties(), toolsProperties);

        ToolRouter router = new ToolRouter(
            List.of(new FixedAgentTool("loan.query", Map.of("totalCount", 1))), properties);
        AgentPolicyEnforcer policyEnforcer = new AgentPolicyEnforcer(properties, router);
        AgentToolExecutor toolExecutor = new AgentToolExecutor(router, properties, new ObjectMapper(), AgentRateLimiter.noop());

        // 限流版 LLM client：rpm=60 → 1 token/sec，acquireTimeoutMs=50 → 第二次 LLM 调用（REFLECT）必被限流
        AgentRateLimitProperties rlProps = new AgentRateLimitProperties();
        rlProps.getLlm().setRpm(60);
        rlProps.getLlm().setConcurrency(10);
        rlProps.getLlm().setAcquireTimeoutMs(50);
        AgentLlmClient rateLimitedLlmClient = new AgentLlmClient(properties, new TokenBucketRateLimiter(rlProps));
        ReflectionTestUtils.setField(rateLimitedLlmClient, "chatClient", chatClient);

        AgentStructuredOutputValidator validator = new AgentStructuredOutputValidator(new ObjectMapper());
        AgentLoopService rateLimitedService = new AgentLoopService(
            intentRoutingService, properties, policyEnforcer, toolExecutor,
            new AgentPlanner(policyEnforcer, router, rateLimitedLlmClient, validator),
            new AgentReflector(rateLimitedLlmClient, validator),
            new AgentResponder(properties, policyEnforcer, rateLimitedLlmClient, validator),
            agentTraceService
        );

        when(callResponseSpec.content())
            .thenReturn("{\"toolCalls\":[{\"toolName\":\"loan.query\",\"args\":{\"userNo\":\"USER1001\"}}],\"done\":false}");

        AgentRequest request = new AgentRequest();
        request.setMessage("请查询 USER1001 的借款记录");

        AgentResponse response = rateLimitedService.chat(request);

        assertTrue(response.getFinalAnswer().contains("基于当前可验证信息，我先给出稳健结论"));
        assertEquals("RATE_LIMITED", response.getFailureReason().name());
        assertFalse(response.isCompleted());
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

    private static class NoopAgentTraceService extends AgentTraceService {
        private NoopAgentTraceService() {
            super(null, null, null);
        }

        @Override
        public boolean persistTrace(AgentRequest request, AgentResponse response, String mode) {
            return true;
        }
    }
}
