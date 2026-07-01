package com.example.aidevelop.agent.service;

public record AgentToolExecutionResult(
    boolean success,
    String outputText,
    String errorMessage,
    int attempts,
    long latencyMs
) {
}
