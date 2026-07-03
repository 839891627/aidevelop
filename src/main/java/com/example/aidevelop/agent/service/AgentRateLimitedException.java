package com.example.aidevelop.agent.service;

/**
 * LLM 或工具调用触发 L3 资源限流时抛出，由 AgentLoopService 捕获后走兜底回答。
 */
public class AgentRateLimitedException extends RuntimeException {

    public AgentRateLimitedException(String message) {
        super(message);
    }
}
