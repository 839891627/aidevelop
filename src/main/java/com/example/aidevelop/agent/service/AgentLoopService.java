package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.AgentActionType;
import com.example.aidevelop.agent.model.AgentFailureReason;
import com.example.aidevelop.agent.model.AgentStepStatus;
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
    private final AgentTraceService agentTraceService;

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
        AgentBudgetTracker budgetTracker = new AgentBudgetTracker(agentProperties);
        budgetTracker.startRound(0);
        AgentTraceContext.set(new AgentTraceContext.Context(traceId, request.getConversationId(), "AGENT", 0, budgetTracker));
        AgentState state = new AgentState(traceId, routePlan, maxSteps, allowedTools, budgetTracker);

        log.info("AgentLoop 开始: traceId={}, routeType={}, maxSteps={}", traceId, routePlan.routeType(), maxSteps);

        try {
        // Plan 阶段：让模型先产出结构化 toolCalls，而不是直接回答。
        AgentPlanResult planResult = agentPlanner.buildPlan(request, routePlan, allowedTools, maxSteps);
        state.addStep(AgentStep.builder()
            .stepIndex(state.nextStepIndex())
            .roundIndex(0)
            .actionType(AgentActionType.PLAN)
            .status(planResult.fromError() ? AgentStepStatus.DEGRADED : AgentStepStatus.SUCCEEDED)
            .failureReason(planResult.fromError() ? AgentFailureReason.PLAN_PARSE_ERROR : AgentFailureReason.NONE)
            .toolOutput(planResult.rawPlan())
            .latencyMs(planResult.latencyMs())
            .success(!planResult.fromError())
            .build());
        if (planResult.fromError()) {
            state.markDegraded(AgentFailureReason.PLAN_PARSE_ERROR);
        }

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
                .roundIndex(0)
                .actionType(AgentActionType.TOOL)
                .status(executionResult.success() ? AgentStepStatus.SUCCEEDED : AgentStepStatus.FAILED)
                .failureReason(executionResult.success() ? AgentFailureReason.NONE : classifyToolFailure(executionResult.errorMessage()))
                .toolName(toolCall.toolName())
                .toolInput(toolCall.args())
                .toolOutput("attempts=%d; result=%s".formatted(executionResult.attempts(), executionResult.outputText()))
                .latencyMs(executionResult.latencyMs())
                .success(executionResult.success())
                .errorMessage(executionResult.success() ? null : executionResult.errorMessage())
                .build());
            if (!executionResult.success()) {
                state.markDegraded(classifyToolFailure(executionResult.errorMessage()));
            }
        }

        // Reflect 阶段：初始 plan 的所有工具执行完毕后，再统一判断证据是否充足。
        if (agentProperties.isReflectEnabled() && !planResult.toolCalls().isEmpty()) {
            AgentReflectDecision decision = agentReflector.reflect(request, routePlan, state.getObservations());
            state.addStep(AgentStep.builder()
                .stepIndex(state.nextStepIndex())
                .roundIndex(0)
                .actionType(AgentActionType.REFLECT)
                .status(AgentStepStatus.SUCCEEDED)
                .failureReason(AgentFailureReason.NONE)
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
            budgetTracker.startRound(replanRound + 1);
            AgentTraceContext.set(new AgentTraceContext.Context(traceId, request.getConversationId(), "REPLAN", replanRound + 1, budgetTracker));
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
                .roundIndex(replanRound)
                .actionType(AgentActionType.PLAN)
                .status(replanResult.fromError() ? AgentStepStatus.DEGRADED : AgentStepStatus.SUCCEEDED)
                .failureReason(replanResult.fromError() ? AgentFailureReason.PLAN_PARSE_ERROR : AgentFailureReason.NONE)
                .toolOutput("replan#" + replanRound + ": " + replanResult.rawPlan())
                .latencyMs(replanResult.latencyMs())
                .success(!replanResult.fromError())
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
                    .roundIndex(replanRound)
                    .actionType(AgentActionType.TOOL)
                    .status(executionResult.success() ? AgentStepStatus.SUCCEEDED : AgentStepStatus.FAILED)
                    .failureReason(executionResult.success() ? AgentFailureReason.NONE : classifyToolFailure(executionResult.errorMessage()))
                    .toolName(toolCall.toolName())
                    .toolInput(toolCall.args())
                    .toolOutput("attempts=%d; result=%s".formatted(executionResult.attempts(), executionResult.outputText()))
                    .latencyMs(executionResult.latencyMs())
                    .success(executionResult.success())
                    .errorMessage(executionResult.success() ? null : executionResult.errorMessage())
                    .build());
                if (!executionResult.success()) {
                    state.markDegraded(classifyToolFailure(executionResult.errorMessage()));
                }

                if (agentProperties.isReflectEnabled()) {
                    AgentReflectDecision decision = agentReflector.reflect(request, routePlan, state.getObservations());
                    state.addStep(AgentStep.builder()
                        .stepIndex(state.nextStepIndex())
                        .roundIndex(replanRound)
                        .actionType(AgentActionType.REFLECT)
                        .status(AgentStepStatus.SUCCEEDED)
                        .failureReason(AgentFailureReason.NONE)
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
                .roundIndex(replanRound)
                .actionType(AgentActionType.REFLECT)
                .status(AgentStepStatus.SUCCEEDED)
                .failureReason(AgentFailureReason.NONE)
                .toolOutput(decision.reason())
                .latencyMs(decision.latencyMs())
                .success(true)
                .build());
        }

        if (agentPolicyEnforcer.shouldSupplementRagEvidence(
            request, allowedTools, state.getObservations(), state.getExecutedToolNames())) {
            executeSupplementalRag(request, state, replanRound);
        }

        long respondStart = System.currentTimeMillis();
        String draftAnswer = agentResponder.buildFinalAnswer(request, routePlan, state.getObservations());
        // SelfCheck 阶段：最终答案输出前再检查证据完整性和回答质量，失败时走兜底回答。
        AgentSelfCheckDecision selfCheckDecision = agentResponder.selfCheck(request, routePlan, state.getObservations(), draftAnswer);
        state.addStep(AgentStep.builder()
            .stepIndex(state.nextStepIndex())
            .roundIndex(replanRound)
            .actionType(AgentActionType.SELF_CHECK)
            .status(selfCheckDecision.pass() ? AgentStepStatus.SUCCEEDED : AgentStepStatus.DEGRADED)
            .failureReason(selfCheckDecision.pass() ? AgentFailureReason.NONE : AgentFailureReason.SELF_CHECK_FAILED)
            .toolOutput("pass=%s; score=%d; reason=%s".formatted(
                selfCheckDecision.pass(), selfCheckDecision.score(), selfCheckDecision.reason()))
            .latencyMs(selfCheckDecision.latencyMs())
            .success(selfCheckDecision.pass())
            .build());
        if (!selfCheckDecision.pass()
            && agentPolicyEnforcer.shouldSupplementRagEvidence(
                request, allowedTools, state.getObservations(), state.getExecutedToolNames())) {
            executeSupplementalRag(request, state, replanRound);
            respondStart = System.currentTimeMillis();
            draftAnswer = agentResponder.buildFinalAnswer(request, routePlan, state.getObservations());
            selfCheckDecision = agentResponder.selfCheck(request, routePlan, state.getObservations(), draftAnswer);
            state.addStep(AgentStep.builder()
                .stepIndex(state.nextStepIndex())
                .roundIndex(replanRound)
                .actionType(AgentActionType.SELF_CHECK)
                .status(selfCheckDecision.pass() ? AgentStepStatus.SUCCEEDED : AgentStepStatus.DEGRADED)
                .failureReason(selfCheckDecision.pass() ? AgentFailureReason.NONE : AgentFailureReason.SELF_CHECK_FAILED)
                .toolOutput("pass=%s; score=%d; reason=%s".formatted(
                    selfCheckDecision.pass(), selfCheckDecision.score(), selfCheckDecision.reason()))
                .latencyMs(selfCheckDecision.latencyMs())
                .success(selfCheckDecision.pass())
                .build());
        }
        if (!selfCheckDecision.pass()) {
            state.markDegraded(AgentFailureReason.SELF_CHECK_FAILED);
        }

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
            .roundIndex(replanRound)
            .actionType(AgentActionType.RESPOND)
            .status(shouldFallback ? AgentStepStatus.DEGRADED : AgentStepStatus.SUCCEEDED)
            .failureReason(shouldFallback ? state.getFailureReason() : AgentFailureReason.NONE)
            .toolOutput(finalAnswer)
            .latencyMs(System.currentTimeMillis() - respondStart)
            .success(true)
            .build());

        long responseTime = System.currentTimeMillis() - startedAt;
        log.info("AgentLoop 完成: traceId={}, steps={}, responseTime={}ms", traceId, state.getSteps().size(), responseTime);
        AgentResponse response = AgentResponse.builder()
            .traceId(traceId)
            .routeType(routePlan.routeType().name())
            .status(state.getStatus())
            .failureReason(state.getFailureReason())
            .finalAnswer(finalAnswer)
            .completed(true)
            .executedSteps(state.getSteps().size())
            .responseTimeMs(responseTime)
            .budgetSummary(budgetTracker.toSummary())
            .steps(state.getSteps())
            .build();
        response.setTracePersisted(agentTraceService.persistTrace(request, response, "SINGLE"));
        return response;
        } catch (AgentLlmTimeoutException ex) {
            state.markFailed(AgentFailureReason.LLM_TIMEOUT);
            long responseTime = System.currentTimeMillis() - startedAt;
            String fallback = agentResponder.buildFallbackAnswer(request, routePlan, state.getObservations(), ex.getMessage(), false);
            AgentResponse response = AgentResponse.builder()
                .traceId(traceId)
                .routeType(routePlan.routeType().name())
                .status(AgentStepStatus.TIMED_OUT)
                .failureReason(AgentFailureReason.LLM_TIMEOUT)
                .finalAnswer(fallback)
                .completed(false)
                .executedSteps(state.getSteps().size())
                .responseTimeMs(responseTime)
                .budgetSummary(budgetTracker.toSummary())
                .steps(state.getSteps())
                .build();
            response.setTracePersisted(agentTraceService.persistTrace(request, response, "SINGLE"));
            return response;
        } catch (AgentBudgetExceededException ex) {
            state.markFailed(AgentFailureReason.BUDGET_EXCEEDED);
            long responseTime = System.currentTimeMillis() - startedAt;
            String fallback = agentResponder.buildFallbackAnswer(request, routePlan, state.getObservations(), ex.getMessage(), false);
            AgentResponse response = AgentResponse.builder()
                .traceId(traceId)
                .routeType(routePlan.routeType().name())
                .status(AgentStepStatus.FAILED)
                .failureReason(AgentFailureReason.BUDGET_EXCEEDED)
                .finalAnswer(fallback)
                .completed(false)
                .executedSteps(state.getSteps().size())
                .responseTimeMs(responseTime)
                .budgetSummary(budgetTracker.toSummary())
                .steps(state.getSteps())
                .build();
            response.setTracePersisted(agentTraceService.persistTrace(request, response, "SINGLE"));
            return response;
        } catch (AgentRateLimitedException ex) {
            state.markFailed(AgentFailureReason.RATE_LIMITED);
            long responseTime = System.currentTimeMillis() - startedAt;
            String fallback = agentResponder.buildFallbackAnswer(request, routePlan, state.getObservations(), ex.getMessage(), false);
            AgentResponse response = AgentResponse.builder()
                .traceId(traceId)
                .routeType(routePlan.routeType().name())
                .status(AgentStepStatus.FAILED)
                .failureReason(AgentFailureReason.RATE_LIMITED)
                .finalAnswer(fallback)
                .completed(false)
                .executedSteps(state.getSteps().size())
                .responseTimeMs(responseTime)
                .budgetSummary(budgetTracker.toSummary())
                .steps(state.getSteps())
                .build();
            response.setTracePersisted(agentTraceService.persistTrace(request, response, "SINGLE"));
            return response;
        } finally {
            AgentTraceContext.clear();
        }
    }

    private int resolveMaxSteps(Integer requestMaxSteps, int routeMaxToolCalls) {
        int fromRequest = requestMaxSteps == null ? agentProperties.getMaxSteps() : requestMaxSteps;
        int bounded = Math.max(1, Math.min(fromRequest, agentProperties.getMaxSteps()));
        return Math.max(1, Math.min(bounded, routeMaxToolCalls));
    }

    private void executeSupplementalRag(AgentRequest request, AgentState state, int roundIndex) {
        ToolCall toolCall = agentPolicyEnforcer.buildRagToolCall(request, state.getRoutePlan());
        AgentToolExecutionResult executionResult = agentToolExecutor.executeWithRetry(toolCall);
        state.recordSupplementalToolCall();
        state.addExecutedToolName(toolCall.toolName());
        String observation = executionResult.success()
            ? toolCall.toolName() + ": " + executionResult.outputText()
            : toolCall.toolName() + " 执行失败: " + executionResult.errorMessage();
        state.addObservation(observation);

        state.addStep(AgentStep.builder()
            .stepIndex(state.nextStepIndex())
            .roundIndex(roundIndex)
            .actionType(AgentActionType.TOOL)
            .status(executionResult.success() ? AgentStepStatus.SUCCEEDED : AgentStepStatus.FAILED)
            .failureReason(executionResult.success() ? AgentFailureReason.NONE : classifyToolFailure(executionResult.errorMessage()))
            .toolName(toolCall.toolName())
            .toolInput(toolCall.args())
            .toolOutput("attempts=%d; result=%s".formatted(executionResult.attempts(), executionResult.outputText()))
            .latencyMs(executionResult.latencyMs())
            .success(executionResult.success())
            .errorMessage(executionResult.success() ? null : executionResult.errorMessage())
            .build());
        if (!executionResult.success()) {
            state.markDegraded(classifyToolFailure(executionResult.errorMessage()));
        }
    }

    private AgentFailureReason classifyToolFailure(String errorMessage) {
        if (errorMessage == null) {
            return AgentFailureReason.TOOL_EXECUTION_FAILED;
        }
        if (errorMessage.contains("超时")) {
            return AgentFailureReason.TOOL_TIMEOUT;
        }
        if (errorMessage.contains("未授权")) {
            return AgentFailureReason.TOOL_UNAUTHORIZED;
        }
        return AgentFailureReason.TOOL_EXECUTION_FAILED;
    }
}
