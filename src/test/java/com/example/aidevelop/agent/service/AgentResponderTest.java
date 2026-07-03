package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.agent.tool.ToolRouter;
import com.example.aidevelop.config.AgentProperties;
import com.example.aidevelop.service.IntentRoutingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentResponderTest {

    @Test
    void shouldRethrowBudgetExceededFromSelfCheck() {
        AgentResponder responder = newResponder(new ThrowingLlmClient(
            new AgentBudgetExceededException("LLM budget exceeded")
        ));

        assertThrows(AgentBudgetExceededException.class, () -> responder.selfCheck(
            request(),
            routePlan(),
            List.of("loan.query: {\"totalCount\":1}"),
            "草稿回答"
        ));
    }

    @Test
    void shouldRethrowLlmTimeoutFromSelfCheck() {
        AgentResponder responder = newResponder(new ThrowingLlmClient(
            new AgentLlmTimeoutException("LLM 调用超时: SELF_CHECK", new TimeoutException("timeout"))
        ));

        assertThrows(AgentLlmTimeoutException.class, () -> responder.selfCheck(
            request(),
            routePlan(),
            List.of("loan.query: {\"totalCount\":1}"),
            "草稿回答"
        ));
    }

    private AgentResponder newResponder(AgentLlmClient llmClient) {
        AgentProperties properties = new AgentProperties();
        ToolRouter toolRouter = new ToolRouter(List.of(), properties);
        AgentPolicyEnforcer policyEnforcer = new AgentPolicyEnforcer(properties, toolRouter);
        AgentStructuredOutputValidator validator = new AgentStructuredOutputValidator(new ObjectMapper());
        return new AgentResponder(properties, policyEnforcer, llmClient, validator);
    }

    private AgentRequest request() {
        AgentRequest request = new AgentRequest();
        request.setMessage("请查询 USER1001 的借款记录");
        return request;
    }

    private IntentRoutingService.RoutePlan routePlan() {
        return new IntentRoutingService.RoutePlan(
            IntentRoutingService.RouteType.TOOL_ONLY,
            false,
            List.of("loanQueryFunction"),
            3,
            0.2,
            3,
            30000,
            "test"
        );
    }

    private static class ThrowingLlmClient extends AgentLlmClient {

        private final RuntimeException exception;

        private ThrowingLlmClient(RuntimeException exception) {
            super(new AgentProperties(), AgentRateLimiter.noop());
            this.exception = exception;
        }

        @Override
        public String call(String phase, String prompt) {
            throw exception;
        }
    }
}
