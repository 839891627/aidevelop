package com.example.aidevelop.agent.service;

public class AgentBudgetExceededException extends RuntimeException {

    public AgentBudgetExceededException(String message) {
        super(message);
    }
}
