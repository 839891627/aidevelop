package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.service.IntentRoutingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AgentReflector {

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("\\{[\\s\\S]*}");

    @Resource(name = "chatClientForOpenAI")
    private ChatClient chatClient;

    private final ObjectMapper objectMapper;

    public AgentReflectDecision reflect(AgentRequest request, IntentRoutingService.RoutePlan routePlan, List<String> observations) {
        long startedAt = System.currentTimeMillis();
        String observationText = observations.isEmpty() ? "暂无观察结果" : String.join("\n", observations);
        String prompt = """
            你是 Agent Reflect 评估器，请判断是否已有足够信息回答用户问题。
            仅返回 JSON：{"done":true/false,"reason":"简短原因"}
            约束：
            1) 若已有明确事实支撑回答，done=true
            2) 若关键信息缺失，done=false
            路由类型：%s
            用户问题：%s
            工具观察：
            %s
            """.formatted(routePlan.routeType(), request.getMessage(), observationText);
        try {
            String raw = chatClient.prompt().user(prompt).call().content();
            JsonNode node = objectMapper.readTree(extractJson(raw));
            boolean done = node.path("done").asBoolean(false);
            String reason = node.path("reason").asText(done ? "信息已充足，可结束工具调用" : "信息不足，继续执行后续步骤");
            return new AgentReflectDecision(done, reason, System.currentTimeMillis() - startedAt);
        } catch (Exception ex) {
            boolean done = !observations.isEmpty();
            String reason = done ? "Reflect 解析失败，按保守策略结束" : "Reflect 解析失败且无观察，继续尝试";
            return new AgentReflectDecision(done, reason, System.currentTimeMillis() - startedAt);
        }
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
