package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.AgentBudgetSummary;
import com.example.aidevelop.config.AgentProperties;

/**
 * Agent 请求级预算跟踪器。当前先跟踪 LLM 调用数、工具调用数和轮次。
 */
public class AgentBudgetTracker {

    private final AgentProperties agentProperties;
    private int llmCalls;
    private int toolCalls;
    private int roundIndex;
    private boolean budgetExceeded;
    private String reason;

    public AgentBudgetTracker(AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
    }

    public void startRound(int nextRoundIndex) {
        this.roundIndex = nextRoundIndex;
        this.llmCalls = 0;
    }

    public void recordLlmCall(String phase) {
        llmCalls++;
        int maxLlmCalls = Math.max(1, agentProperties.getMaxLlmCallsPerRound());
        if (llmCalls > maxLlmCalls) {
            budgetExceeded = true;
            reason = "轮次 %d 的 LLM 调用超过上限 %d，phase=%s".formatted(roundIndex, maxLlmCalls, phase);
            throw new AgentBudgetExceededException(reason);
        }
    }

    public void recordToolCall() {
        toolCalls++;
    }

    public AgentBudgetSummary toSummary() {
        return AgentBudgetSummary.builder()
            .llmCalls(llmCalls)
            .toolCalls(toolCalls)
            .roundIndex(roundIndex)
            .budgetExceeded(budgetExceeded)
            .reason(reason)
            .build();
    }
}
