package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.ToolCall;

import java.util.List;

public record AgentPlanResult(
    List<ToolCall> toolCalls,
    String rawPlan,
    long latencyMs,
    boolean fromError
) {
}
