package com.example.aidevelop.agent.service;

public record AgentSelfCheckDecision(
    boolean pass,
    int score,
    String reason,
    long latencyMs
) {
}
