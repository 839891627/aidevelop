package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.AgentFailureReason;
import com.example.aidevelop.agent.model.ToolCall;
import com.example.aidevelop.agent.multi.SupervisorDecision;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agent LLM 输出结构化校验器。统一 JSON 提取、字段校验和类型转换。
 */
@Component
@RequiredArgsConstructor
public class AgentStructuredOutputValidator {

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("\\{[\\s\\S]*}");

    private final ObjectMapper objectMapper;

    public List<ToolCall> parseToolCalls(String rawPlan, List<String> allowedTools, int maxSteps) {
        JsonNode root = parseRoot(rawPlan, AgentFailureReason.PLAN_PARSE_ERROR);
        JsonNode toolCalls = root.get("toolCalls");
        if (toolCalls == null || !toolCalls.isArray()) {
            throw new AgentStructuredOutputException(
                AgentFailureReason.STRUCTURED_OUTPUT_INVALID,
                "Planner 输出缺少数组字段 toolCalls"
            );
        }

        List<ToolCall> parsed = new ArrayList<>();
        for (JsonNode node : toolCalls) {
            String toolName = node.path("toolName").asText();
            if (toolName == null || toolName.isBlank()) {
                continue;
            }
            if (!allowedTools.contains(toolName)) {
                continue;
            }
            if (!node.has("args") || !node.get("args").isObject()) {
                throw new AgentStructuredOutputException(
                    AgentFailureReason.STRUCTURED_OUTPUT_INVALID,
                    "工具调用缺少对象字段 args: " + toolName
                );
            }
            Map<String, Object> args = objectMapper.convertValue(node.path("args"), Map.class);
            parsed.add(new ToolCall(toolName, args));
            if (parsed.size() >= maxSteps) {
                break;
            }
        }
        return parsed;
    }

    public AgentReflectDecision parseReflectDecision(String raw, long latencyMs) {
        JsonNode root = parseRoot(raw, AgentFailureReason.REFLECT_PARSE_ERROR);
        if (!root.has("done")) {
            throw new AgentStructuredOutputException(
                AgentFailureReason.STRUCTURED_OUTPUT_INVALID,
                "Reflect 输出缺少 done 字段"
            );
        }
        boolean done = root.path("done").asBoolean(false);
        String reason = root.path("reason").asText(done ? "信息已充足，可结束工具调用" : "信息不足，继续执行后续步骤");
        return new AgentReflectDecision(done, reason, latencyMs);
    }

    public AgentSelfCheckDecision parseSelfCheckDecision(String raw, int minScore, long latencyMs) {
        JsonNode root = parseRoot(raw, AgentFailureReason.STRUCTURED_OUTPUT_INVALID);
        int score = Math.max(0, Math.min(100, root.path("score").asInt(0)));
        boolean pass = root.path("pass").asBoolean(score >= minScore);
        String reason = root.path("reason").asText(pass ? "自检通过" : "自检未通过");
        return new AgentSelfCheckDecision(pass, score, reason, latencyMs);
    }

    public SupervisorDecision parseSupervisorDecision(String raw) {
        JsonNode root = parseRoot(raw, AgentFailureReason.SUPERVISOR_DECISION_ERROR);
        String action = root.path("action").asText(SupervisorDecision.FINISH);
        if (!SupervisorDecision.DISPATCH.equalsIgnoreCase(action) && !SupervisorDecision.FINISH.equalsIgnoreCase(action)) {
            throw new AgentStructuredOutputException(
                AgentFailureReason.SUPERVISOR_DECISION_ERROR,
                "Supervisor action 非法: " + action
            );
        }
        String targetAgent = root.path("targetAgent").asText(null);
        String reason = root.path("reason").asText("");
        return new SupervisorDecision(action, targetAgent, reason);
    }

    private JsonNode parseRoot(String raw, AgentFailureReason failureReason) {
        try {
            return objectMapper.readTree(extractJson(raw));
        } catch (Exception ex) {
            throw new AgentStructuredOutputException(failureReason, "JSON 解析失败: " + ex.getMessage());
        }
    }

    private String extractJson(String text) {
        if (text == null || text.isBlank()) {
            return "{}";
        }
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(text.trim());
        if (matcher.find()) {
            return matcher.group();
        }
        return text.trim();
    }
}
