package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.config.AgentProperties;
import com.example.aidevelop.service.IntentRoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentResponder {

    private final AgentProperties agentProperties;
    private final AgentPolicyEnforcer agentPolicyEnforcer;
    private final AgentLlmClient agentLlmClient;
    private final AgentStructuredOutputValidator structuredOutputValidator;

    public String buildFinalAnswer(AgentRequest request, IntentRoutingService.RoutePlan routePlan, List<String> observations) {
        String observationText = observations.isEmpty()
            ? "无工具观察结果，请基于已有上下文谨慎回答。"
            : String.join("\n", observations);
        String responderPrompt = """
            你是企业 AI 助手，请基于工具观察结果回答用户问题。
            要求：
            1) 事实优先引用观察结果
            2) 若观察不足，明确说明不确定部分
            3) 输出中文，简洁清晰
            路由类型：%s
            用户问题：%s
            工具观察：
            %s
            """.formatted(routePlan.routeType(), request.getMessage(), observationText);
        return agentLlmClient.call("RESPOND", responderPrompt);
    }

    public AgentSelfCheckDecision selfCheck(AgentRequest request, IntentRoutingService.RoutePlan routePlan,
                                            List<String> observations, String draftAnswer) {
        long startedAt = System.currentTimeMillis();
        if (!agentProperties.isSelfCheckEnabled()) {
            return new AgentSelfCheckDecision(true, 100, "未启用自检", System.currentTimeMillis() - startedAt);
        }

        String observationText = observations.isEmpty() ? "暂无观察结果" : String.join("\n", observations);
        String prompt = """
            你是 Agent 结果自检器，请评估当前答案是否可靠。
            仅返回 JSON：{"pass":true/false,"score":0-100,"reason":"简短原因"}
            判定重点：
            1) 回答是否与工具观察一致
            2) 是否存在明显信息缺失
            用户问题：%s
            路由类型：%s
            工具观察：
            %s
            当前回答：
            %s
            """.formatted(request.getMessage(), routePlan.routeType(), observationText, draftAnswer);
        try {
            String raw = agentLlmClient.call("SELF_CHECK", prompt);
            AgentSelfCheckDecision parsed = structuredOutputValidator.parseSelfCheckDecision(
                raw, agentProperties.getSelfCheckMinScore(), System.currentTimeMillis() - startedAt);
            int score = parsed.score();
            boolean pass = parsed.pass();
            String reason = parsed.reason();
            if (agentProperties.isRequireRagEvidenceForRisk()
                && agentPolicyEnforcer.isRiskIntent(request.getMessage())
                && !agentPolicyEnforcer.hasRagEvidence(observations)) {
                pass = false;
                score = Math.min(score, agentProperties.getSelfCheckMinScore() - 10);
                reason = "风险问题缺少知识库证据（rag.search）";
            }
            return new AgentSelfCheckDecision(pass, score, reason, System.currentTimeMillis() - startedAt);
        } catch (Exception ex) {
            boolean pass = observations.size() >= agentProperties.getMinObservationCount();
            int score = pass ? agentProperties.getSelfCheckMinScore() : 0;
            String reason = pass ? "自检解析失败，按规则通过" : "自检解析失败且证据不足";
            if (agentProperties.isRequireRagEvidenceForRisk()
                && agentPolicyEnforcer.isRiskIntent(request.getMessage())
                && !agentPolicyEnforcer.hasRagEvidence(observations)) {
                pass = false;
                score = 0;
                reason = "自检解析失败且风险问题缺少 RAG 证据";
            }
            return new AgentSelfCheckDecision(pass, score, reason, System.currentTimeMillis() - startedAt);
        }
    }

    public boolean shouldFallback(boolean replanFailed, AgentSelfCheckDecision selfCheckDecision,
                                  int observationCount, IntentRoutingService.RoutePlan routePlan) {
        if (replanFailed && agentProperties.isFallbackOnReplanFailure()) {
            return true;
        }
        if (agentProperties.isFallbackWhenSelfCheckFailed() && !selfCheckDecision.pass()) {
            return true;
        }
        if (observationCount < agentProperties.getMinObservationCount()
            && routePlan.routeType() != IntentRoutingService.RouteType.RAG_ONLY) {
            return true;
        }
        return selfCheckDecision.score() < agentProperties.getSelfCheckMinScore();
    }

    public String buildFallbackAnswer(AgentRequest request, IntentRoutingService.RoutePlan routePlan,
                                      List<String> observations, String selfCheckReason, boolean replanFailed) {
        String template = agentProperties.getFallbackAnswerTemplate();
        String observationText = observations.isEmpty() ? "暂无可验证工具观察" : String.join(" | ", observations);
        String reason = replanFailed ? "二次改计划失败；" + selfCheckReason : selfCheckReason;
        return template
            .replace("{question}", safeText(request.getMessage()))
            .replace("{routeType}", routePlan.routeType().name())
            .replace("{observations}", safeText(observationText))
            .replace("{reason}", safeText(reason));
    }

    private String safeText(String text) {
        if (text == null || text.isBlank()) {
            return "N/A";
        }
        return text;
    }
}
