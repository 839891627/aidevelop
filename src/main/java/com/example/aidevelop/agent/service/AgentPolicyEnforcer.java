package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.agent.model.ToolCall;
import com.example.aidevelop.agent.tool.ToolRouter;
import com.example.aidevelop.config.AgentProperties;
import com.example.aidevelop.service.IntentRoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agent 策略层：统一处理工具白名单、legacy 工具名映射和强制证据规则。
 */
@Service
@RequiredArgsConstructor
public class AgentPolicyEnforcer {

    private static final Pattern USER_NO_PATTERN = Pattern.compile("(CUST\\d+)", Pattern.CASE_INSENSITIVE);

    private final AgentProperties agentProperties;
    private final ToolRouter toolRouter;

    public List<String> resolveAllowedTools(IntentRoutingService.RoutePlan routePlan, String message) {
        List<String> tools = new ArrayList<>();
        if (routePlan.ragEnabled()) {
            tools.add("rag.search");
        }
        for (String legacyName : routePlan.allowedToolNames()) {
            String mapped = mapLegacyToolName(legacyName);
            if (mapped != null) {
                tools.add(mapped);
            }
        }
        if (tools.isEmpty()) {
            tools.addAll(agentProperties.getAllowedTools());
        }
        if (isRiskIntent(message) && agentProperties.isForceRagForRiskEvaluation() && toolRouter.exists("rag.search")) {
            tools.add("rag.search");
        }
        return tools.stream().distinct().toList();
    }

    public List<ToolCall> enforcePolicyTools(AgentRequest request, IntentRoutingService.RoutePlan routePlan,
                                             List<String> allowedTools, List<ToolCall> plannedCalls,
                                             List<String> executedToolNames, int maxSteps) {
        List<ToolCall> required = new ArrayList<>();
        if (isRiskIntent(request.getMessage())) {
            if (agentProperties.isForceRagForRiskEvaluation()
                && allowedTools.contains("rag.search")
                && !executedToolNames.contains("rag.search")
                && toolRouter.exists("rag.search")) {
                Map<String, Object> ragArgs = new LinkedHashMap<>();
                ragArgs.put("query", request.getMessage());
                ragArgs.put("conversationId", request.getConversationId());
                ragArgs.put("topK", routePlan.ragTopK());
                required.add(new ToolCall("rag.search", ragArgs));
            }
            String userNo = extractUserNo(request.getMessage());
            if (userNo != null
                && allowedTools.contains("risk.assess")
                && !executedToolNames.contains("risk.assess")
                && toolRouter.exists("risk.assess")) {
                required.add(new ToolCall("risk.assess", Map.of("userNo", userNo)));
            }
        }
        return mergeToolCalls(required, plannedCalls, maxSteps);
    }

    public List<ToolCall> fallbackPlan(AgentRequest request, IntentRoutingService.RoutePlan routePlan, int maxSteps) {
        List<ToolCall> fallbackCalls = new ArrayList<>();

        if (routePlan.ragEnabled()) {
            Map<String, Object> ragArgs = new LinkedHashMap<>();
            ragArgs.put("query", request.getMessage());
            ragArgs.put("conversationId", request.getConversationId());
            ragArgs.put("topK", routePlan.ragTopK());
            fallbackCalls.add(new ToolCall("rag.search", ragArgs));
        }

        boolean mentionedRepayment = request.getMessage() != null && request.getMessage().contains("还款");
        String toolName = mentionedRepayment ? "repayment.query" : "loan.query";
        if (routePlan.routeType() != IntentRoutingService.RouteType.RAG_ONLY) {
            Map<String, Object> args = new LinkedHashMap<>();
            String userNo = extractUserNo(request.getMessage());
            if (userNo != null) {
                args.put("userNo", userNo);
            }
            fallbackCalls.add(new ToolCall(toolName, args));
        }

        return fallbackCalls.stream()
            .filter(call -> toolRouter.exists(call.toolName()))
            .limit(maxSteps)
            .toList();
    }

    public boolean isRiskIntent(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase();
        for (String keyword : agentProperties.getRiskIntentKeywords()) {
            if (keyword != null && !keyword.isBlank() && normalized.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRagEvidence(List<String> observations) {
        return observations.stream().anyMatch(obs -> obs != null && obs.startsWith("rag.search:"));
    }

    private List<ToolCall> mergeToolCalls(List<ToolCall> required, List<ToolCall> planned, int maxSteps) {
        List<ToolCall> merged = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (ToolCall call : required) {
            if (!names.contains(call.toolName())) {
                merged.add(call);
                names.add(call.toolName());
            }
        }
        for (ToolCall call : planned) {
            if (!names.contains(call.toolName())) {
                merged.add(call);
                names.add(call.toolName());
            }
        }
        if (merged.size() > maxSteps) {
            return merged.subList(0, maxSteps);
        }
        return merged;
    }

    private String mapLegacyToolName(String legacyName) {
        return switch (legacyName) {
            case "loanQueryFunction" -> "loan.query";
            case "repaymentQueryFunction" -> "repayment.query";
            case "riskAssessmentFunction" -> "risk.assess";
            default -> null;
        };
    }

    private String extractUserNo(String message) {
        if (message == null) {
            return null;
        }
        Matcher matcher = USER_NO_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }
        return null;
    }
}
