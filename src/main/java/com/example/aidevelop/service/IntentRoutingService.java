package com.example.aidevelop.service;

import com.example.aidevelop.config.RouteProperties;
import com.example.aidevelop.config.ToolsProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 可运营路由服务：输出结构化 RoutePlan，供执行层统一消费。
 */
@Service
public class IntentRoutingService {

    private final RouteProperties routeProperties;
    private final ToolsProperties toolsProperties;

    public IntentRoutingService(RouteProperties routeProperties, ToolsProperties toolsProperties) {
        this.routeProperties = routeProperties;
        this.toolsProperties = toolsProperties;
    }

    public RoutePlan plan(String message) {
        if (message == null || message.isBlank()) {
            return buildRagOnlyPlan("空消息默认走 RAG");
        }

        String normalized = message.trim().toLowerCase();

        if (isMultiAgentRequired(normalized)) {
            return buildMultiAgentPlan("命中多 Agent 协作规则");
        }

        Pattern businessIdPattern = Pattern.compile(routeProperties.getBusinessIdPattern(), Pattern.CASE_INSENSITIVE);
        if (businessIdPattern.matcher(message).find() || containsAny(normalized, routeProperties.getToolIntentKeywords())) {
            return buildToolOnlyPlan("命中业务查询规则，优先工具调用");
        }
        if (containsAny(normalized, routeProperties.getRagIntentKeywords())) {
            return buildRagOnlyPlan("命中知识问答规则，优先 RAG");
        }
        return buildHybridPlan("默认走 HYBRID（工具+RAG）");
    }

    private boolean isMultiAgentRequired(String normalized) {
        List<String> keywords = routeProperties.getMultiAgentKeywords();
        if (keywords == null || keywords.isEmpty()) {
            return false;
        }
        if (containsAny(normalized, keywords)) {
            return true;
        }
        boolean hitsTool = containsAny(normalized, routeProperties.getToolIntentKeywords());
        boolean hitsRag = containsAny(normalized, routeProperties.getRagIntentKeywords());
        return hitsTool && hitsRag;
    }

    private RoutePlan buildMultiAgentPlan(String reason) {
        return new RoutePlan(
            RouteType.MULTI_AGENT,
            true,
            resolveAllowedToolNames(routeProperties.getHybridToolNames()),
            routeProperties.getHybridTopK(),
            routeProperties.getHybridSimilarityThreshold(),
            routeProperties.getMaxToolCalls(),
            routeProperties.getTimeoutMs(),
            reason
        );
    }

    private RoutePlan buildToolOnlyPlan(String reason) {
        return new RoutePlan(
            RouteType.TOOL_ONLY,
            false,
            resolveAllowedToolNames(routeProperties.getToolOnlyToolNames()),
            routeProperties.getHybridTopK(),
            routeProperties.getHybridSimilarityThreshold(),
            routeProperties.getMaxToolCalls(),
            routeProperties.getTimeoutMs(),
            reason
        );
    }

    private RoutePlan buildRagOnlyPlan(String reason) {
        return new RoutePlan(
            RouteType.RAG_ONLY,
            true,
            List.of(),
            routeProperties.getRagOnlyTopK(),
            routeProperties.getRagOnlySimilarityThreshold(),
            routeProperties.getMaxToolCalls(),
            routeProperties.getTimeoutMs(),
            reason
        );
    }

    private RoutePlan buildHybridPlan(String reason) {
        return new RoutePlan(
            RouteType.HYBRID,
            true,
            resolveAllowedToolNames(routeProperties.getHybridToolNames()),
            routeProperties.getHybridTopK(),
            routeProperties.getHybridSimilarityThreshold(),
            routeProperties.getMaxToolCalls(),
            routeProperties.getTimeoutMs(),
            reason
        );
    }

    private List<String> resolveAllowedToolNames(List<String> configured) {
        List<String> enabled = toolsProperties.getEnabled();
        if (enabled == null || enabled.isEmpty()) {
            return configured == null ? List.of() : configured;
        }
        if (configured == null || configured.isEmpty()) {
            return enabled;
        }
        List<String> intersection = new ArrayList<>();
        for (String name : configured) {
            if (enabled.contains(name)) {
                intersection.add(name);
            }
        }
        return intersection;
    }

    private boolean containsAny(String source, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && source.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public enum RouteType {
        TOOL_ONLY,
        RAG_ONLY,
        HYBRID,
        MULTI_AGENT
    }

    public record RoutePlan(
        RouteType routeType,
        boolean ragEnabled,
        List<String> allowedToolNames,
        int ragTopK,
        double ragSimilarityThreshold,
        int maxToolCalls,
        int timeoutMs,
        String reason
    ) {
    }
}
