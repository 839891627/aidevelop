package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.AgentActionType;
import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.agent.model.AgentResponse;
import com.example.aidevelop.agent.model.AgentStep;
import com.example.aidevelop.agent.model.ToolCall;
import com.example.aidevelop.config.AgentProperties;
import com.example.aidevelop.exception.AiServiceException;
import com.example.aidevelop.service.IntentRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentLoopService implements AgentService {

    private final IntentRoutingService intentRoutingService;
    private final AgentProperties agentProperties;
    private final AgentPolicyEnforcer agentPolicyEnforcer;
    private final AgentToolExecutor agentToolExecutor;
    private final AgentPlanner agentPlanner;
    private final AgentReflector agentReflector;
    private final AgentResponder agentResponder;

    @Override
    public AgentResponse chat(AgentRequest request) {
        if (!agentProperties.isEnabled()) {
            throw new AiServiceException("Agent 功能未启用");
        }

        long startedAt = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString();
        IntentRoutingService.RoutePlan routePlan = intentRoutingService.plan(request.getMessage());
        int maxSteps = resolveMaxSteps(request.getMaxSteps(), routePlan.maxToolCalls());
        List<String> allowedTools = agentPolicyEnforcer.resolveAllowedTools(routePlan, request.getMessage());
        AgentState state = new AgentState(traceId, routePlan, maxSteps, allowedTools);

        log.info("AgentLoop 开始: traceId={}, routeType={}, maxSteps={}", traceId, routePlan.routeType(), maxSteps);

        AgentPlanResult planResult = agentPlanner.buildPlan(request, routePlan, allowedTools, maxSteps);
        state.addStep(AgentStep.builder()
            .stepIndex(state.nextStepIndex())
            .actionType(AgentActionType.PLAN)
            .toolOutput(planResult.rawPlan())
            .latencyMs(planResult.latencyMs())
            .success(true)
            .build());

        for (ToolCall toolCall : planResult.toolCalls()) {
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
                .toolOutput("attempts=%d; result=%s".formatted(executionResult.attempts(), executionResult.outputText()))
                .latencyMs(executionResult.latencyMs())
                .success(executionResult.success())
                .errorMessage(executionResult.success() ? null : executionResult.errorMessage())
                .build());

            if (agentProperties.isReflectEnabled()) {
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
        }

        int replanRound = 0;
        while (!state.isShouldStop() && agentProperties.isReplanEnabled()
            && replanRound < Math.max(0, agentProperties.getMaxReplanRounds())
            && state.getExecutedToolCalls() < maxSteps) {
            int remainingSteps = maxSteps - state.getExecutedToolCalls();
            AgentPlanResult replanResult = agentPlanner.buildReplan(
                request,
                routePlan,
                allowedTools,
                remainingSteps,
                state.getObservations(),
                state.getExecutedToolNames()
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

            for (ToolCall toolCall : replanResult.toolCalls()) {
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
                    .toolOutput("attempts=%d; result=%s".formatted(executionResult.attempts(), executionResult.outputText()))
                    .latencyMs(executionResult.latencyMs())
                    .success(executionResult.success())
                    .errorMessage(executionResult.success() ? null : executionResult.errorMessage())
                    .build());

                if (agentProperties.isReflectEnabled()) {
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

                if (state.getExecutedToolCalls() >= maxSteps) {
                    break;
                }
            }
        }

        if (!state.isShouldStop() && planResult.toolCalls().isEmpty() && agentProperties.isReflectEnabled()) {
            AgentReflectDecision decision = agentReflector.reflect(request, routePlan, state.getObservations());
            state.addStep(AgentStep.builder()
                .stepIndex(state.nextStepIndex())
                .actionType(AgentActionType.REFLECT)
                .toolOutput(decision.reason())
                .latencyMs(decision.latencyMs())
                .success(true)
                .build());
        }

        long respondStart = System.currentTimeMillis();
        String draftAnswer = agentResponder.buildFinalAnswer(request, routePlan, state.getObservations());
        AgentSelfCheckDecision selfCheckDecision = agentResponder.selfCheck(request, routePlan, state.getObservations(), draftAnswer);
        state.addStep(AgentStep.builder()
            .stepIndex(state.nextStepIndex())
            .actionType(AgentActionType.SELF_CHECK)
            .toolOutput("pass=%s; score=%d; reason=%s".formatted(
                selfCheckDecision.pass(), selfCheckDecision.score(), selfCheckDecision.reason()))
            .latencyMs(selfCheckDecision.latencyMs())
            .success(true)
            .build());

        boolean shouldFallback = agentResponder.shouldFallback(
            state.isReplanFailed(),
            selfCheckDecision,
            state.getObservations().size(),
            routePlan
        );
        String finalAnswer = shouldFallback
            ? agentResponder.buildFallbackAnswer(request, routePlan, state.getObservations(), selfCheckDecision.reason(), state.isReplanFailed())
            : draftAnswer;
        state.addStep(AgentStep.builder()
            .stepIndex(state.nextStepIndex())
            .actionType(AgentActionType.RESPOND)
            .toolOutput(finalAnswer)
            .latencyMs(System.currentTimeMillis() - respondStart)
            .success(true)
            .build());

        long responseTime = System.currentTimeMillis() - startedAt;
        log.info("AgentLoop 完成: traceId={}, steps={}, responseTime={}ms", traceId, state.getSteps().size(), responseTime);
        return AgentResponse.builder()
            .traceId(traceId)
            .routeType(routePlan.routeType().name())
            .finalAnswer(finalAnswer)
            .completed(true)
            .executedSteps(state.getSteps().size())
            .responseTimeMs(responseTime)
            .steps(state.getSteps())
            .build();
    }

    private int resolveMaxSteps(Integer requestMaxSteps, int routeMaxToolCalls) {
        int fromRequest = requestMaxSteps == null ? agentProperties.getMaxSteps() : requestMaxSteps;
        int bounded = Math.max(1, Math.min(fromRequest, agentProperties.getMaxSteps()));
        return Math.max(1, Math.min(bounded, routeMaxToolCalls));
    }
}
