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
        // Agent 仍复用 IntentRoutingService：先判断问题需要工具、RAG、混合还是多 Agent。
        IntentRoutingService.RoutePlan routePlan = intentRoutingService.plan(request.getMessage());
        int maxSteps = resolveMaxSteps(request.getMaxSteps(), routePlan.maxToolCalls());
        // 策略层会结合路由结果和用户问题，收敛本轮 Agent 真正允许调用的工具集合。
        List<String> allowedTools = agentPolicyEnforcer.resolveAllowedTools(routePlan, request.getMessage());
        AgentState state = new AgentState(traceId, routePlan, maxSteps, allowedTools);

        log.info("AgentLoop 开始: traceId={}, routeType={}, maxSteps={}", traceId, routePlan.routeType(), maxSteps);

        // Plan 阶段：让模型先产出结构化 toolCalls，而不是直接回答。
        AgentPlanResult planResult = agentPlanner.buildPlan(request, routePlan, allowedTools, maxSteps);
        state.addStep(AgentStep.builder()
            .stepIndex(state.nextStepIndex())
            .actionType(AgentActionType.PLAN)
            .toolOutput(planResult.rawPlan())
            .latencyMs(planResult.latencyMs())
            .success(true)
            .build());

        for (ToolCall toolCall : planResult.toolCalls()) {
            // Tool 阶段：逐个执行计划中的工具调用，并把结果沉淀为 observation。
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
        }

        // Reflect 阶段：初始 plan 的所有工具执行完毕后，再统一判断证据是否充足。
        if (agentProperties.isReflectEnabled() && !planResult.toolCalls().isEmpty()) {
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
            }
        }

        int replanRound = 0;
        while (!state.isShouldStop() && agentProperties.isReplanEnabled()
            && replanRound < Math.max(0, agentProperties.getMaxReplanRounds())
            && state.getExecutedToolCalls() < maxSteps) {
            int remainingSteps = maxSteps - state.getExecutedToolCalls();
            // Replan 阶段：如果反思后发现证据不足，基于已有 observation 继续规划补充工具调用。
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
                // 重新规划得到的工具调用也进入同一套 Tool -> Observe -> Reflect 流程。
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
            // 没有规划出工具时也做一次反思，方便记录“为何无需工具/为何证据不足”。
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
        // SelfCheck 阶段：最终答案输出前再检查证据完整性和回答质量，失败时走兜底回答。
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
