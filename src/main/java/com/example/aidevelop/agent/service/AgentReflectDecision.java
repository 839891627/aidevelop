package com.example.aidevelop.agent.service;

public record AgentReflectDecision(
    boolean done,
    String reason,
    long latencyMs
) {
}
