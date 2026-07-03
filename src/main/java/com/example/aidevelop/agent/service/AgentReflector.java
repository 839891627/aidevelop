package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.service.IntentRoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentReflector {

    private final AgentLlmClient agentLlmClient;
    private final AgentStructuredOutputValidator structuredOutputValidator;

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
            String raw = agentLlmClient.call("REFLECT", prompt);
            return structuredOutputValidator.parseReflectDecision(raw, System.currentTimeMillis() - startedAt);
        } catch (Exception ex) {
            boolean done = !observations.isEmpty();
            String reason = done ? "Reflect 解析失败，按保守策略结束" : "Reflect 解析失败且无观察，继续尝试";
            return new AgentReflectDecision(done, reason, System.currentTimeMillis() - startedAt);
        }
    }
}
