package com.example.aidevelop.agent.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Agent 运行预算摘要")
public class AgentBudgetSummary {

    private int llmCalls;
    private int toolCalls;
    private int roundIndex;
    private boolean budgetExceeded;
    private String reason;
}
