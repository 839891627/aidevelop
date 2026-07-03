package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.AgentFailureReason;

public class AgentStructuredOutputException extends RuntimeException {

    private final AgentFailureReason failureReason;

    public AgentStructuredOutputException(AgentFailureReason failureReason, String message) {
        super(message);
        this.failureReason = failureReason;
    }

    public AgentFailureReason getFailureReason() {
        return failureReason;
    }
}
