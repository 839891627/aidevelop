package com.example.aidevelop.agent.multi;

import com.example.aidevelop.agent.model.AgentActionType;
import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.agent.model.AgentResponse;
import com.example.aidevelop.agent.model.AgentStep;
import com.example.aidevelop.agent.service.AgentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class SupervisorOrchestrator implements AgentService {

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("\\{[\\s\\S]*}");

    private final SubAgentRunner subAgentRunner;
    private final MultiAgentProperties multiAgentProperties;
    private final ObjectMapper objectMapper;

    @Resource(name = "chatClientForOpenAI")
    private ChatClient chatClient;

    @Override
    public AgentResponse chat(AgentRequest request) {
        long startedAt = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString();
        MultiAgentState state = new MultiAgentState(traceId);

        log.info("Supervisor 开始编排: traceId={}, message={}", traceId, request.getMessage());

        int maxRounds = multiAgentProperties.getMaxSupervisorRounds();

        for (int round = 1; round <= maxRounds; round++) {
            long decisionStart = System.currentTimeMillis();
            SupervisorDecision decision = decideNext(request, state);

            state.addSteps(java.util.List.of(AgentStep.builder()
                .stepIndex(state.getTotalExecutedSteps() + 1)
                .actionType(AgentActionType.DELEGATE)
                .toolName("supervisor")
                .toolOutput("round=%d; action=%s; target=%s; reason=%s".formatted(
                    round, decision.action(), decision.targetAgent(), decision.reason()))
                .latencyMs(System.currentTimeMillis() - decisionStart)
                .success(true)
                .build()));

            if (decision.isFinish()) {
                log.info("Supervisor 决定 FINISH: round={}, reason={}", round, decision.reason());
                break;
            }

            String targetAgent = decision.targetAgent();
            if (targetAgent == null || !multiAgentProperties.getAgents().containsKey(targetAgent)) {
                log.warn("Supervisor 指定了未知 Agent: {}, 终止编排", targetAgent);
                break;
            }

            SubAgentDefinition definition = multiAgentProperties.toDefinition(targetAgent);
            long subStart = System.currentTimeMillis();
            try {
                AgentResponse subResponse = subAgentRunner.execute(request, definition, state);
                long subLatency = System.currentTimeMillis() - subStart;

                state.put(definition.outputKey(), subResponse.getFinalAnswer());
                state.recordExecution(new SubAgentExecution(
                    targetAgent, definition.outputKey(), subResponse, subLatency));

                log.info("SubAgent [{}] 执行完成: round={}, latency={}ms", targetAgent, round, subLatency);
            } catch (Exception ex) {
                long subLatency = System.currentTimeMillis() - subStart;
                String errorMsg = targetAgent + " 执行异常: " + ex.getMessage();
                state.put(definition.outputKey(), errorMsg);
                log.warn("SubAgent [{}] 执行失败: round={}, latency={}ms, error={}",
                    targetAgent, round, subLatency, ex.getMessage());
            }
        }

        String finalAnswer = synthesize(request, state);
        long responseTime = System.currentTimeMillis() - startedAt;

        log.info("Supervisor 编排完成: traceId={}, totalSteps={}, subAgents={}, responseTime={}ms",
            traceId, state.getTotalExecutedSteps(), state.getExecutions().size(), responseTime);

        return AgentResponse.builder()
            .traceId(traceId)
            .routeType("MULTI_AGENT")
            .finalAnswer(finalAnswer)
            .completed(true)
            .executedSteps(state.getTotalExecutedSteps())
            .responseTimeMs(responseTime)
            .steps(state.getAllSteps())
            .subAgentExecutions(state.getExecutions())
            .build();
    }

    private SupervisorDecision decideNext(AgentRequest request, MultiAgentState state) {
        String agentDescriptions = multiAgentProperties.getAgents().entrySet().stream()
            .map(e -> e.getKey() + " (" + e.getValue().getDescription() + ")")
            .collect(Collectors.joining(", "));

        String contextSummary = state.buildContextSummary();

        String systemPrompt = multiAgentProperties.getSupervisorSystemPrompt();
        if (systemPrompt == null || systemPrompt.isBlank()) {
            systemPrompt = """
                你是多 Agent 调度器。根据用户问题和已有结果决定下一步。
                仅返回 JSON：{"action":"DISPATCH","targetAgent":"<name>","reason":"..."} 或 {"action":"FINISH","reason":"..."}
                """;
        }

        String prompt = """
            %s

            可用 Agent: %s
            已有结果: %s
            用户问题: %s
            """.formatted(systemPrompt, agentDescriptions, contextSummary, request.getMessage());

        try {
            String raw = chatClient.prompt().user(prompt).call().content();
            return parseSupervisorDecision(raw);
        } catch (Exception ex) {
            log.warn("Supervisor 决策失败，终止编排: {}", ex.getMessage());
            return new SupervisorDecision(SupervisorDecision.FINISH, null, "决策异常: " + ex.getMessage());
        }
    }

    private String synthesize(AgentRequest request, MultiAgentState state) {
        if (state.getExecutions().isEmpty()) {
            return "多 Agent 编排未产生有效结果";
        }

        if (state.getExecutions().size() == 1) {
            return state.getExecutions().get(0).response().getFinalAnswer();
        }

        String contextSummary = state.buildContextSummary();
        String prompt = """
            请基于以下多个 Agent 的执行结果，为用户生成一份综合、完整的回答。
            要求：事实优先引用各 Agent 结果，简洁清晰，中文输出。

            用户问题: %s

            各 Agent 结果:
            %s
            """.formatted(request.getMessage(), contextSummary);

        try {
            return chatClient.prompt().user(prompt).call().content();
        } catch (Exception ex) {
            log.warn("Supervisor 综合生成失败，拼接原始结果: {}", ex.getMessage());
            return state.getExecutions().stream()
                .map(e -> "【" + e.agentName() + "】\n" + e.response().getFinalAnswer())
                .collect(Collectors.joining("\n\n"));
        }
    }

    private SupervisorDecision parseSupervisorDecision(String raw) {
        String json = extractJson(raw);
        try {
            JsonNode node = objectMapper.readTree(json);
            String action = node.path("action").asText(SupervisorDecision.FINISH);
            String targetAgent = node.path("targetAgent").asText(null);
            String reason = node.path("reason").asText("");
            return new SupervisorDecision(action, targetAgent, reason);
        } catch (Exception ex) {
            log.warn("Supervisor 决策 JSON 解析失败: {}", ex.getMessage());
            return new SupervisorDecision(SupervisorDecision.FINISH, null, "JSON 解析失败");
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
