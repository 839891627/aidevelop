package com.example.aidevelop.agent.multi;

public record SupervisorDecision(
    String action,
    String targetAgent,
    String reason
) {
    public static final String DISPATCH = "DISPATCH";
    public static final String FINISH = "FINISH";

    public boolean isFinish() {
        return FINISH.equalsIgnoreCase(action);
    }
}
