package com.example.aidevelop.agent.multi;

import com.example.aidevelop.agent.model.AgentActionType;
import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.agent.model.AgentResponse;
import com.example.aidevelop.agent.model.AgentStep;
import com.example.aidevelop.agent.model.ToolCall;
import com.example.aidevelop.agent.service.AgentPlanResult;
import com.example.aidevelop.agent.service.AgentPlanner;
import com.example.aidevelop.agent.service.AgentReflectDecision;
import com.example.aidevelop.agent.service.AgentReflector;
import com.example.aidevelop.agent.service.AgentResponder;
import com.example.aidevelop.agent.service.AgentSelfCheckDecision;
import com.example.aidevelop.agent.service.AgentState;
import com.example.aidevelop.agent.service.AgentToolExecutionResult;
import com.example.aidevelop.agent.service.AgentToolExecutor;
import com.example.aidevelop.service.IntentRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubAgentRunner {

    private final AgentPlanner agentPlanner;
    private final AgentToolExecutor agentToolExecutor;
    private final AgentReflector agentReflector;
    private final AgentResponder agentResponder;

    public AgentResponse execute(AgentRequest request, SubAgentDefinition definition, MultiAgentState sharedState) {
        long startedAt = System.currentTimeMillis();
        String traceId = sharedState.getTraceId() + "/" + definition.name();

        IntentRoutingService.RoutePlan routePlan = buildSyntheticRoutePlan(definition);
        List<String> allowedTools = definition.allowedTools();
        int maxSteps = definition.maxSteps();

        AgentState state = new AgentState(traceId, routePlan, maxSteps, allowedTools);

        log.info("SubAgent [{}] 开始执行: traceId={}, maxSteps={}, tools={}",
            definition.name(), traceId, maxSteps, allowedTools);

        AgentRequest enrichedRequest = enrichRequestWithContext(request, definition, sharedState);

        AgentPlanResult planResult = agentPlanner.buildPlan(enrichedRequest, routePlan, allowedTools, maxSteps);
        state.addStep(AgentStep.builder()
            .stepIndex(state.nextStepIndex())
            .actionType(AgentActionType.PLAN)
            .toolOutput(planResult.rawPlan())
            .latencyMs(planResult.latencyMs())
            .success(true)
            .build());

        executeToolCalls(enrichedRequest, routePlan, definition, state, planResult.toolCalls());

        int replanRound = 0;
        int maxReplanRounds = definition.replanEnabled() ? 1 : 0;
        while (!state.isShouldStop() && definition.replanEnabled()
            && replanRound < maxReplanRounds
            && state.getExecutedToolCalls() < maxSteps) {

            int remainingSteps = maxSteps - state.getExecutedToolCalls();
            AgentPlanResult replanResult = agentPlanner.buildReplan(
                enrichedRequest, routePlan, allowedTools, remainingSteps,
                state.getObservations(), state.getExecutedToolNames()
            );
            if (replanResult.toolCalls().isEmpty()) {
                if (replanResult.fromError()) {
                    state.markReplanFailed();
                }
                break;
            }
            replanRound++;
            state.addStep(AgentStep.builder()
                .stepIndex(state.nextStepIndex())
                .actionType(AgentActionType.PLAN)
                .toolOutput("replan#" + replanRound + ": " + replanResult.rawPlan())
                .latencyMs(replanResult.latencyMs())
                .success(true)
                .build());

            executeToolCalls(enrichedRequest, routePlan, definition, state, replanResult.toolCalls());
        }

        long respondStart = System.currentTimeMillis();
        String draftAnswer = agentResponder.buildFinalAnswer(enrichedRequest, routePlan, state.getObservations());

        if (definition.selfCheckEnabled()) {
            AgentSelfCheckDecision selfCheck = agentResponder.selfCheck(
                enrichedRequest, routePlan, state.getObservations(), draftAnswer);
            state.addStep(AgentStep.builder()
                .stepIndex(state.nextStepIndex())
                .actionType(AgentActionType.SELF_CHECK)
                .toolOutput("pass=%s; score=%d; reason=%s".formatted(
                    selfCheck.pass(), selfCheck.score(), selfCheck.reason()))
                .latencyMs(selfCheck.latencyMs())
                .success(true)
                .build());

            if (!selfCheck.pass()) {
                draftAnswer = agentResponder.buildFallbackAnswer(
                    enrichedRequest, routePlan, state.getObservations(),
                    selfCheck.reason(), state.isReplanFailed());
            }
        }

        state.addStep(AgentStep.builder()
            .stepIndex(state.nextStepIndex())
            .actionType(AgentActionType.RESPOND)
            .toolOutput(draftAnswer)
            .latencyMs(System.currentTimeMillis() - respondStart)
            .success(true)
            .build());

        long responseTime = System.currentTimeMillis() - startedAt;
        log.info("SubAgent [{}] 完成: steps={}, responseTime={}ms",
            definition.name(), state.getSteps().size(), responseTime);

        sharedState.addSteps(state.getSteps());

        return AgentResponse.builder()
            .traceId(traceId)
            .routeType(routePlan.routeType().name())
            .finalAnswer(draftAnswer)
            .completed(true)
            .executedSteps(state.getSteps().size())
            .responseTimeMs(responseTime)
            .steps(state.getSteps())
            .build();
    }

    private void executeToolCalls(AgentRequest request, IntentRoutingService.RoutePlan routePlan,
                                   SubAgentDefinition definition, AgentState state, List<ToolCall> toolCalls) {
        for (ToolCall toolCall : toolCalls) {
            AgentToolExecutionResult executionResult = agentToolExecutor.executeWithRetry(toolCall);
            state.incrementExecutedToolCalls();
            state.addExecutedToolName(toolCall.toolName());

            String observation = executionResult.success()
                ? toolCall.toolName() + ": " + executionResult.outputText()
                : toolCall.toolName() + " 执行失败: " + executionResult.errorMessage();
            state.addObservation(observation);

            state.addStep(AgentStep.builder()
                .stepIndex(state.nextStepIndex())
                .actionType(AgentActionType.TOOL)
                .toolName(toolCall.toolName())
                .toolInput(toolCall.args())
                .toolOutput("attempts=%d; result=%s".formatted(
                    executionResult.attempts(), executionResult.outputText()))
                .latencyMs(executionResult.latencyMs())
                .success(executionResult.success())
                .errorMessage(executionResult.success() ? null : executionResult.errorMessage())
                .build());

            if (definition.reflectEnabled()) {
                AgentReflectDecision decision = agentReflector.reflect(request, routePlan, state.getObservations());
                state.addStep(AgentStep.builder()
                    .stepIndex(state.nextStepIndex())
                    .actionType(AgentActionType.REFLECT)
                    .toolOutput(decision.reason())
                    .latencyMs(decision.latencyMs())
                    .success(true)
                    .build());
                if (decision.done()) {
                    state.markShouldStop();
                    break;
                }
            }

            if (state.getExecutedToolCalls() >= state.getMaxSteps()) {
                break;
            }
        }
    }

    private AgentRequest enrichRequestWithContext(AgentRequest original, SubAgentDefinition definition,
                                                   MultiAgentState sharedState) {
        String contextSummary = sharedState.buildContextSummary();
        if ("暂无已有结果".equals(contextSummary)) {
            return original;
        }
        AgentRequest enriched = new AgentRequest();
        enriched.setMessage(original.getMessage() + "\n\n【已有上下文】\n" + contextSummary);
        enriched.setConversationId(original.getConversationId());
        enriched.setMaxSteps(definition.maxSteps());
        return enriched;
    }

    private IntentRoutingService.RoutePlan buildSyntheticRoutePlan(SubAgentDefinition definition) {
        boolean ragEnabled = definition.allowedTools().contains("rag.search");
        return new IntentRoutingService.RoutePlan(
            IntentRoutingService.RouteType.TOOL_ONLY,
            ragEnabled,
            definition.allowedTools(),
            3,
            0.2,
            definition.maxSteps(),
            15000,
            "sub-agent: " + definition.name()
        );
    }
}
