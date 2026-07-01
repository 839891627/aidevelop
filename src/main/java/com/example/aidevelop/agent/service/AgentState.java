package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.AgentStep;
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

    public AgentState(String traceId, IntentRoutingService.RoutePlan routePlan, int maxSteps, List<String> allowedTools) {
        this.traceId = traceId;
        this.routePlan = routePlan;
        this.maxSteps = maxSteps;
        this.allowedTools = allowedTools;
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
    }

    public void markShouldStop() {
        shouldStop = true;
    }

    public void markReplanFailed() {
        replanFailed = true;
    }
}
