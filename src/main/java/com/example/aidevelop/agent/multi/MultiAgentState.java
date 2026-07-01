package com.example.aidevelop.agent.multi;

import com.example.aidevelop.agent.model.AgentStep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MultiAgentState {

    private final String traceId;
    private final ConcurrentHashMap<String, Object> sharedData = new ConcurrentHashMap<>();
    private final List<AgentStep> allSteps = Collections.synchronizedList(new ArrayList<>());
    private final List<SubAgentExecution> executions = Collections.synchronizedList(new ArrayList<>());

    public MultiAgentState(String traceId) {
        this.traceId = traceId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void put(String key, Object value) {
        if (key != null && value != null) {
            sharedData.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = sharedData.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    public ConcurrentHashMap<String, Object> getSharedData() {
        return sharedData;
    }

    public String buildContextSummary() {
        if (sharedData.isEmpty()) {
            return "暂无已有结果";
        }
        StringBuilder sb = new StringBuilder();
        sharedData.forEach((key, value) -> {
            sb.append("【").append(key).append("】: ");
            String text = String.valueOf(value);
            if (text.length() > 500) {
                text = text.substring(0, 500) + "...";
            }
            sb.append(text).append("\n");
        });
        return sb.toString();
    }

    public void addSteps(List<AgentStep> steps) {
        allSteps.addAll(steps);
    }

    public List<AgentStep> getAllSteps() {
        return Collections.unmodifiableList(allSteps);
    }

    public void recordExecution(SubAgentExecution execution) {
        executions.add(execution);
    }

    public List<SubAgentExecution> getExecutions() {
        return Collections.unmodifiableList(executions);
    }

    public int getTotalExecutedSteps() {
        return allSteps.size();
    }
}
