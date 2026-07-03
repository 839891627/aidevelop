package com.example.aidevelop.agent.service;

public class AgentLlmTimeoutException extends RuntimeException {

    public AgentLlmTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
