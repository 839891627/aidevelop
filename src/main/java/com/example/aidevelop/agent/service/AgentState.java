package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.AgentStep;
import com.example.aidevelop.agent.model.AgentFailureReason;
import com.example.aidevelop.agent.model.AgentStepStatus;
import com.example.aidevelop.service.IntentRoutingService;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class AgentState {

    private final String traceId;
    private final IntentRoutingService.RoutePlan routePlan;
    private final int maxSteps;
    private final List<String> allowedTools;
    private final List<AgentStep> steps = new ArrayList<>();
    private final List<String> observations = new ArrayList<>();
    private final List<String> executedToolNames = new ArrayList<>();
    private int stepIndex = 1;
    private int executedToolCalls;
    private boolean shouldStop;
    private boolean replanFailed;
    private final AgentBudgetTracker budgetTracker;
    private AgentStepStatus status = AgentStepStatus.SUCCEEDED;
    private AgentFailureReason failureReason = AgentFailureReason.NONE;

    public AgentState(String traceId, IntentRoutingService.RoutePlan routePlan, int maxSteps,
                      List<String> allowedTools, AgentBudgetTracker budgetTracker) {
        this.traceId = traceId;
        this.routePlan = routePlan;
        this.maxSteps = maxSteps;
        this.allowedTools = allowedTools;
        this.budgetTracker = budgetTracker;
    }

    public int nextStepIndex() {
        return stepIndex++;
    }

    public void addStep(AgentStep step) {
        steps.add(step);
    }

    public void addObservation(String observation) {
        observations.add(observation);
    }

    public void addExecutedToolName(String toolName) {
        executedToolNames.add(toolName);
    }

    public void incrementExecutedToolCalls() {
        executedToolCalls++;
        if (budgetTracker != null) {
            budgetTracker.recordToolCall();
        }
    }

    public void markShouldStop() {
        shouldStop = true;
    }

    public void markReplanFailed() {
        replanFailed = true;
        markDegraded(AgentFailureReason.REPLAN_EXHAUSTED);
    }

    public void markFailed(AgentFailureReason reason) {
        status = AgentStepStatus.FAILED;
        failureReason = reason == null ? AgentFailureReason.UNKNOWN : reason;
    }

    public void markDegraded(AgentFailureReason reason) {
        if (status != AgentStepStatus.FAILED) {
            status = AgentStepStatus.DEGRADED;
            failureReason = reason == null ? AgentFailureReason.UNKNOWN : reason;
        }
    }
}
