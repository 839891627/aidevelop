package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.AgentActionType;
import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.agent.model.AgentResponse;
import com.example.aidevelop.agent.model.AgentStep;
import com.example.aidevelop.agent.model.ToolCall;
import com.example.aidevelop.agent.tool.ToolRouter;
import com.example.aidevelop.config.AgentProperties;
import com.example.aidevelop.exception.AiServiceException;
import com.example.aidevelop.service.IntentRoutingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentLoopService implements AgentService {

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("\\{[\\s\\S]*}");
    private static final Pattern USER_NO_PATTERN = Pattern.compile("(CUST\\d+)", Pattern.CASE_INSENSITIVE);
    @Resource(name = "chatClientForOpenAI")
    private ChatClient chatClient;

    private final IntentRoutingService intentRoutingService;
    private final ToolRouter toolRouter;
    private final ObjectMapper objectMapper;
    private final AgentProperties agentProperties;

    @Override
    public AgentResponse chat(AgentRequest request) {
        if (!agentProperties.isEnabled()) {
            throw new AiServiceException("Agent 功能未启用");
        }

        long startedAt = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString();
        IntentRoutingService.RoutePlan routePlan = intentRoutingService.plan(request.getMessage());
        int maxSteps = resolveMaxSteps(request.getMaxSteps(), routePlan.maxToolCalls());
        List<String> allowedTools = resolveAllowedTools(routePlan, request.getMessage());
        List<AgentStep> steps = new ArrayList<>();
        List<String> observations = new ArrayList<>();
        List<String> executedToolNames = new ArrayList<>();

        log.info("AgentLoop 开始: traceId={}, routeType={}, maxSteps={}", traceId, routePlan.routeType(), maxSteps);

        PlanResult planResult = buildPlan(request, routePlan, allowedTools, maxSteps);
        steps.add(AgentStep.builder()
            .stepIndex(1)
            .actionType(AgentActionType.PLAN)
            .toolOutput(planResult.rawPlan())
            .latencyMs(planResult.latencyMs())
            .success(true)
            .build());

        int stepIndex = 2;
        boolean shouldStop = false;
        int executedToolCalls = 0;
        boolean replanFailed = false;
        for (ToolCall toolCall : planResult.toolCalls()) {
            ToolExecutionResult executionResult = executeToolWithRetry(toolCall);
            executedToolCalls++;
            executedToolNames.add(toolCall.toolName());
            String observation = executionResult.success()
                ? toolCall.toolName() + ": " + executionResult.outputText()
                : toolCall.toolName() + " 执行失败: " + executionResult.errorMessage();
            observations.add(observation);

            steps.add(AgentStep.builder()
                .stepIndex(stepIndex++)
                .actionType(AgentActionType.TOOL)
                .toolName(toolCall.toolName())
                .toolInput(toolCall.args())
                .toolOutput("attempts=%d; result=%s".formatted(executionResult.attempts(), executionResult.outputText()))
                .latencyMs(executionResult.latencyMs())
                .success(executionResult.success())
                .errorMessage(executionResult.success() ? null : executionResult.errorMessage())
                .build());

            if (agentProperties.isReflectEnabled()) {
                ReflectDecision decision = reflect(request, routePlan, observations);
                steps.add(AgentStep.builder()
                    .stepIndex(stepIndex++)
                    .actionType(AgentActionType.REFLECT)
                    .toolOutput(decision.reason())
                    .latencyMs(decision.latencyMs())
                    .success(true)
                    .build());
                if (decision.done()) {
                    shouldStop = true;
                    break;
                }
            }
        }

        int replanRound = 0;
        while (!shouldStop && agentProperties.isReplanEnabled()
            && replanRound < Math.max(0, agentProperties.getMaxReplanRounds())
            && executedToolCalls < maxSteps) {
            int remainingSteps = maxSteps - executedToolCalls;
            PlanResult replanResult = buildReplan(request, routePlan, allowedTools, remainingSteps, observations, executedToolNames);
            if (replanResult.toolCalls().isEmpty()) {
                replanFailed = replanResult.fromError();
                break;
            }
            replanRound++;
            steps.add(AgentStep.builder()
                .stepIndex(stepIndex++)
                .actionType(AgentActionType.PLAN)
                .toolOutput("replan#" + replanRound + ": " + replanResult.rawPlan())
                .latencyMs(replanResult.latencyMs())
                .success(true)
                .build());

            for (ToolCall toolCall : replanResult.toolCalls()) {
                ToolExecutionResult executionResult = executeToolWithRetry(toolCall);
                executedToolCalls++;
                executedToolNames.add(toolCall.toolName());
                String observation = executionResult.success()
                    ? toolCall.toolName() + ": " + executionResult.outputText()
                    : toolCall.toolName() + " 执行失败: " + executionResult.errorMessage();
                observations.add(observation);

                steps.add(AgentStep.builder()
                    .stepIndex(stepIndex++)
                    .actionType(AgentActionType.TOOL)
                    .toolName(toolCall.toolName())
                    .toolInput(toolCall.args())
                    .toolOutput("attempts=%d; result=%s".formatted(executionResult.attempts(), executionResult.outputText()))
                    .latencyMs(executionResult.latencyMs())
                    .success(executionResult.success())
                    .errorMessage(executionResult.success() ? null : executionResult.errorMessage())
                    .build());

                if (agentProperties.isReflectEnabled()) {
                    ReflectDecision decision = reflect(request, routePlan, observations);
                    steps.add(AgentStep.builder()
                        .stepIndex(stepIndex++)
                        .actionType(AgentActionType.REFLECT)
                        .toolOutput(decision.reason())
                        .latencyMs(decision.latencyMs())
                        .success(true)
                        .build());
                    if (decision.done()) {
                        shouldStop = true;
                        break;
                    }
                }

                if (executedToolCalls >= maxSteps) {
                    break;
                }
            }
        }

        if (!shouldStop && planResult.toolCalls().isEmpty() && agentProperties.isReflectEnabled()) {
            ReflectDecision decision = reflect(request, routePlan, observations);
            steps.add(AgentStep.builder()
                .stepIndex(stepIndex++)
                .actionType(AgentActionType.REFLECT)
                .toolOutput(decision.reason())
                .latencyMs(decision.latencyMs())
                .success(true)
                .build());
        }

        long respondStart = System.currentTimeMillis();
        String draftAnswer = buildFinalAnswer(request, routePlan, observations);
        SelfCheckDecision selfCheckDecision = selfCheck(request, routePlan, observations, draftAnswer);
        steps.add(AgentStep.builder()
            .stepIndex(stepIndex++)
            .actionType(AgentActionType.SELF_CHECK)
            .toolOutput("pass=%s; score=%d; reason=%s".formatted(
                selfCheckDecision.pass(), selfCheckDecision.score(), selfCheckDecision.reason()))
            .latencyMs(selfCheckDecision.latencyMs())
            .success(true)
            .build());

        boolean shouldFallback = shouldFallback(replanFailed, selfCheckDecision, observations.size(), routePlan);
        String finalAnswer = shouldFallback
            ? buildFallbackAnswer(request, routePlan, observations, selfCheckDecision.reason(), replanFailed)
            : draftAnswer;
        steps.add(AgentStep.builder()
            .stepIndex(stepIndex)
            .actionType(AgentActionType.RESPOND)
            .toolOutput(finalAnswer)
            .latencyMs(System.currentTimeMillis() - respondStart)
            .success(true)
            .build());

        long responseTime = System.currentTimeMillis() - startedAt;
        log.info("AgentLoop 完成: traceId={}, steps={}, responseTime={}ms", traceId, steps.size(), responseTime);
        return AgentResponse.builder()
            .traceId(traceId)
            .routeType(routePlan.routeType().name())
            .finalAnswer(finalAnswer)
            .completed(true)
            .executedSteps(steps.size())
            .responseTimeMs(responseTime)
            .steps(steps)
            .build();
    }

    private PlanResult buildPlan(AgentRequest request, IntentRoutingService.RoutePlan routePlan, List<String> allowedTools, int maxSteps) {
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
            plannedCalls = enforcePolicyTools(request, routePlan, allowedTools, plannedCalls, List.of(), maxSteps);
            if (!plannedCalls.isEmpty()) {
                return new PlanResult(plannedCalls, raw, System.currentTimeMillis() - startedAt, false);
            }
            List<ToolCall> fallback = fallbackPlan(request, routePlan, maxSteps);
            fallback = enforcePolicyTools(request, routePlan, allowedTools, fallback, List.of(), maxSteps);
            return new PlanResult(fallback, raw, System.currentTimeMillis() - startedAt, false);
        } catch (Exception ex) {
            log.warn("Agent Planner 降级到规则计划: {}", ex.getMessage());
            List<ToolCall> fallback = fallbackPlan(request, routePlan, maxSteps);
            fallback = enforcePolicyTools(request, routePlan, allowedTools, fallback, List.of(), maxSteps);
            return new PlanResult(fallback, "planner-fallback", System.currentTimeMillis() - startedAt, true);
        }
    }

    private PlanResult buildReplan(AgentRequest request, IntentRoutingService.RoutePlan routePlan, List<String> allowedTools,
                                   int remainingSteps, List<String> observations, List<String> executedToolNames) {
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
            planned = enforcePolicyTools(request, routePlan, allowedTools, planned, executedToolNames, remainingSteps);
            return new PlanResult(planned, raw, System.currentTimeMillis() - startedAt, false);
        } catch (Exception ex) {
            log.warn("Agent Replan 失败，跳过本轮: {}", ex.getMessage());
            return new PlanResult(List.of(), "replan-fallback", System.currentTimeMillis() - startedAt, true);
        }
    }

    private List<String> resolveAllowedTools(IntentRoutingService.RoutePlan routePlan, String message) {
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

    private List<ToolCall> enforcePolicyTools(AgentRequest request, IntentRoutingService.RoutePlan routePlan,
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
            if (!allowedTools.contains(toolName) || !toolRouter.exists(toolName)) {
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

    private List<ToolCall> fallbackPlan(AgentRequest request, IntentRoutingService.RoutePlan routePlan, int maxSteps) {
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

    private String buildFinalAnswer(AgentRequest request, IntentRoutingService.RoutePlan routePlan, List<String> observations) {
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
        return chatClient.prompt().user(responderPrompt).call().content();
    }

    private ToolExecutionResult executeToolWithRetry(ToolCall toolCall) {
        int totalAttempts = Math.max(1, agentProperties.getToolMaxRetries() + 1);
        long startedAt = System.currentTimeMillis();
        String lastError = null;
        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                Object result = executeToolWithTimeout(toolCall);
                return new ToolExecutionResult(
                    true,
                    toJson(result),
                    null,
                    attempt,
                    System.currentTimeMillis() - startedAt
                );
            } catch (Exception ex) {
                lastError = ex.getMessage() == null ? "工具执行失败" : ex.getMessage();
                if (attempt < totalAttempts && agentProperties.getRetryBackoffMs() > 0) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(agentProperties.getRetryBackoffMs());
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.warn("Agent tool 执行失败(已重试): toolName={}, attempts={}, error={}",
            toolCall.toolName(), totalAttempts, lastError);
        return new ToolExecutionResult(false, "FAILED", lastError, totalAttempts, System.currentTimeMillis() - startedAt);
    }

    private Object executeToolWithTimeout(ToolCall toolCall) {
        try {
            return CompletableFuture
                .supplyAsync(() -> toolRouter.execute(toolCall.toolName(), toolCall.args()))
                .orTimeout(agentProperties.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof TimeoutException) {
                throw new AiServiceException("工具调用超时: " + toolCall.toolName(), cause);
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new AiServiceException("工具调用失败: " + toolCall.toolName(), cause);
        }
    }

    private ReflectDecision reflect(AgentRequest request, IntentRoutingService.RoutePlan routePlan, List<String> observations) {
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
            return new ReflectDecision(done, reason, System.currentTimeMillis() - startedAt);
        } catch (Exception ex) {
            boolean done = !observations.isEmpty();
            String reason = done ? "Reflect 解析失败，按保守策略结束" : "Reflect 解析失败且无观察，继续尝试";
            return new ReflectDecision(done, reason, System.currentTimeMillis() - startedAt);
        }
    }

    private SelfCheckDecision selfCheck(AgentRequest request, IntentRoutingService.RoutePlan routePlan,
                                        List<String> observations, String draftAnswer) {
        long startedAt = System.currentTimeMillis();
        if (!agentProperties.isSelfCheckEnabled()) {
            return new SelfCheckDecision(true, 100, "未启用自检", System.currentTimeMillis() - startedAt);
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
            String raw = chatClient.prompt().user(prompt).call().content();
            JsonNode node = objectMapper.readTree(extractJson(raw));
            int score = Math.max(0, Math.min(100, node.path("score").asInt(0)));
            boolean pass = node.path("pass").asBoolean(score >= agentProperties.getSelfCheckMinScore());
            String reason = node.path("reason").asText(pass ? "自检通过" : "自检未通过");
            if (agentProperties.isRequireRagEvidenceForRisk()
                && isRiskIntent(request.getMessage())
                && !hasRagEvidence(observations)) {
                pass = false;
                score = Math.min(score, agentProperties.getSelfCheckMinScore() - 10);
                reason = "风险问题缺少知识库证据（rag.search）";
            }
            return new SelfCheckDecision(pass, score, reason, System.currentTimeMillis() - startedAt);
        } catch (Exception ex) {
            boolean pass = observations.size() >= agentProperties.getMinObservationCount();
            int score = pass ? agentProperties.getSelfCheckMinScore() : 0;
            String reason = pass ? "自检解析失败，按规则通过" : "自检解析失败且证据不足";
            if (agentProperties.isRequireRagEvidenceForRisk()
                && isRiskIntent(request.getMessage())
                && !hasRagEvidence(observations)) {
                pass = false;
                score = 0;
                reason = "自检解析失败且风险问题缺少 RAG 证据";
            }
            return new SelfCheckDecision(pass, score, reason, System.currentTimeMillis() - startedAt);
        }
    }

    private boolean shouldFallback(boolean replanFailed, SelfCheckDecision selfCheckDecision,
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

    private String buildFallbackAnswer(AgentRequest request, IntentRoutingService.RoutePlan routePlan,
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

    private int resolveMaxSteps(Integer requestMaxSteps, int routeMaxToolCalls) {
        int fromRequest = requestMaxSteps == null ? agentProperties.getMaxSteps() : requestMaxSteps;
        int bounded = Math.max(1, Math.min(fromRequest, agentProperties.getMaxSteps()));
        return Math.max(1, Math.min(bounded, routeMaxToolCalls));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
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

    private String mapLegacyToolName(String legacyName) {
        return switch (legacyName) {
            case "loanQueryFunction" -> "loan.query";
            case "repaymentQueryFunction" -> "repayment.query";
            case "riskAssessmentFunction" -> "risk.assess";
            default -> null;
        };
    }

    private boolean isRiskIntent(String message) {
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

    private boolean hasRagEvidence(List<String> observations) {
        return observations.stream().anyMatch(obs -> obs != null && obs.startsWith("rag.search:"));
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

    private record PlanResult(
        List<ToolCall> toolCalls,
        String rawPlan,
        long latencyMs,
        boolean fromError
    ) {
    }

    private record ReflectDecision(
        boolean done,
        String reason,
        long latencyMs
    ) {
    }

    private record ToolExecutionResult(
        boolean success,
        String outputText,
        String errorMessage,
        int attempts,
        long latencyMs
    ) {
    }

    private record SelfCheckDecision(
        boolean pass,
        int score,
        String reason,
        long latencyMs
    ) {
    }
}
