package com.example.aidevelop.agent.service;

/**
 * Agent 调用链上下文，用于把 traceId、phase 和预算状态传递给 LLM 调用及 AOP 日志。
 */
public final class AgentTraceContext {

    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private AgentTraceContext() {
    }

    public static void set(Context context) {
        CURRENT.set(context);
    }

    public static Context get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static Context withPhase(String phase) {
        Context current = CURRENT.get();
        if (current == null) {
            return null;
        }
        Context updated = current.withPhase(phase);
        CURRENT.set(updated);
        return updated;
    }

    public record Context(
        String traceId,
        String conversationId,
        String phase,
        int roundIndex,
        AgentBudgetTracker budgetTracker
    ) {
        public Context withPhase(String nextPhase) {
            return new Context(traceId, conversationId, nextPhase, roundIndex, budgetTracker);
        }

        public Context withRound(int nextRoundIndex) {
            return new Context(traceId, conversationId, phase, nextRoundIndex, budgetTracker);
        }
    }
}
