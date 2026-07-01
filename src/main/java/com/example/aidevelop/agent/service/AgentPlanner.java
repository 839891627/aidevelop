package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.agent.model.ToolCall;
import com.example.aidevelop.service.IntentRoutingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentPlanner {

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("\\{[\\s\\S]*}");

    @Resource(name = "chatClientForOpenAI")
    private ChatClient chatClient;

    private final ObjectMapper objectMapper;
    private final AgentPolicyEnforcer agentPolicyEnforcer;

    public AgentPlanResult buildPlan(AgentRequest request, IntentRoutingService.RoutePlan routePlan,
                                     List<String> allowedTools, int maxSteps) {
        long startedAt = System.currentTimeMillis();
        String plannerPrompt = """
            你是 Agent 规划器。请根据用户问题返回工具调用计划。
            约束：
            1) 只能使用以下工具：%s
            2) 最多返回 %d 个 toolCalls
            3) 仅输出 JSON，不要输出 markdown
            4) JSON 格式：{"toolCalls":[{"toolName":"...","args":{...}}],"done":false}
            5) 如果无需工具，返回 {"toolCalls":[],"done":true}
            用户问题：%s
            """.formatted(allowedTools, maxSteps, request.getMessage());

        try {
            String raw = chatClient.prompt().user(plannerPrompt).call().content();
            List<ToolCall> plannedCalls = parseToolCalls(raw, allowedTools, maxSteps);
            plannedCalls = agentPolicyEnforcer.enforcePolicyTools(request, routePlan, allowedTools, plannedCalls, List.of(), maxSteps);
            if (!plannedCalls.isEmpty()) {
                return new AgentPlanResult(plannedCalls, raw, System.currentTimeMillis() - startedAt, false);
            }
            List<ToolCall> fallback = agentPolicyEnforcer.fallbackPlan(request, routePlan, maxSteps);
            fallback = agentPolicyEnforcer.enforcePolicyTools(request, routePlan, allowedTools, fallback, List.of(), maxSteps);
            return new AgentPlanResult(fallback, raw, System.currentTimeMillis() - startedAt, false);
        } catch (Exception ex) {
            log.warn("Agent Planner 降级到规则计划: {}", ex.getMessage());
            List<ToolCall> fallback = agentPolicyEnforcer.fallbackPlan(request, routePlan, maxSteps);
            fallback = agentPolicyEnforcer.enforcePolicyTools(request, routePlan, allowedTools, fallback, List.of(), maxSteps);
            return new AgentPlanResult(fallback, "planner-fallback", System.currentTimeMillis() - startedAt, true);
        }
    }

    public AgentPlanResult buildReplan(AgentRequest request, IntentRoutingService.RoutePlan routePlan,
                                       List<String> allowedTools, int remainingSteps,
                                       List<String> observations, List<String> executedToolNames) {
        long startedAt = System.currentTimeMillis();
        String prompt = """
            你是 Agent 二次规划器。请根据已有观察决定是否补充工具调用。
            约束：
            1) 只能使用工具：%s
            2) 已调用过的工具：%s
            3) 最多返回 %d 个 toolCalls
            4) 只输出 JSON：{"toolCalls":[{"toolName":"...","args":{...}}],"done":true/false}
            路由类型：%s
            用户问题：%s
            现有观察：
            %s
            """.formatted(allowedTools, executedToolNames, remainingSteps, routePlan.routeType(), request.getMessage(),
            observations.isEmpty() ? "暂无观察" : String.join("\n", observations));
        try {
            String raw = chatClient.prompt().user(prompt).call().content();
            List<ToolCall> planned = parseToolCalls(raw, allowedTools, remainingSteps);
            planned = agentPolicyEnforcer.enforcePolicyTools(request, routePlan, allowedTools, planned, executedToolNames, remainingSteps);
            return new AgentPlanResult(planned, raw, System.currentTimeMillis() - startedAt, false);
        } catch (Exception ex) {
            log.warn("Agent Replan 失败，跳过本轮: {}", ex.getMessage());
            return new AgentPlanResult(List.of(), "replan-fallback", System.currentTimeMillis() - startedAt, true);
        }
    }

    private List<ToolCall> parseToolCalls(String rawPlan, List<String> allowedTools, int maxSteps) {
        String jsonText = extractJson(rawPlan);
        JsonNode root;
        try {
            root = objectMapper.readTree(jsonText);
        } catch (Exception ex) {
            log.warn("Planner 输出解析失败: {}", ex.getMessage());
            return List.of();
        }
        JsonNode toolCalls = root.get("toolCalls");
        if (toolCalls == null || !toolCalls.isArray()) {
            return List.of();
        }

        List<ToolCall> parsed = new ArrayList<>();
        for (JsonNode node : toolCalls) {
            String toolName = node.path("toolName").asText();
            if (!allowedTools.contains(toolName)) {
                continue;
            }
            Map<String, Object> args = objectMapper.convertValue(node.path("args"), Map.class);
            parsed.add(new ToolCall(toolName, args));
            if (parsed.size() >= maxSteps) {
                break;
            }
        }
        return parsed;
    }

    private String extractJson(String text) {
        if (text == null || text.isBlank()) {
            return "{}";
        }
        String trimmed = text.trim();
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group();
        }
        return trimmed;
    }
}
