package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.agent.model.ToolCall;
import com.example.aidevelop.agent.tool.ToolRouter;
import com.example.aidevelop.service.IntentRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentPlanner {

    private final AgentPolicyEnforcer agentPolicyEnforcer;
    private final ToolRouter toolRouter;
    private final AgentLlmClient agentLlmClient;
    private final AgentStructuredOutputValidator structuredOutputValidator;

    public AgentPlanResult buildPlan(AgentRequest request, IntentRoutingService.RoutePlan routePlan,
                                     List<String> allowedTools, int maxSteps) {
        long startedAt = System.currentTimeMillis();
        String toolDescriptions = toolRouter.buildToolDescriptions(allowedTools);
        String plannerPrompt = """
            你是 Agent 规划器。请根据用户问题返回工具调用计划。
            约束：
            1) 只能使用以下工具：
            %s
            2) 参数必须严格使用工具描述中定义的枚举值，不要使用中文别名
            3) 最多返回 %d 个 toolCalls
            4) 仅输出 JSON，不要输出 markdown
            5) JSON 格式：{"toolCalls":[{"toolName":"...","args":{...}}],"done":false}
            6) 如果无需工具，返回 {"toolCalls":[],"done":true}
            用户问题：%s
            """.formatted(toolDescriptions, maxSteps, request.getMessage());

        try {
            String raw = agentLlmClient.call("PLAN", plannerPrompt);
            List<ToolCall> plannedCalls = structuredOutputValidator.parseToolCalls(raw, allowedTools, maxSteps);
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
        String toolDescriptions = toolRouter.buildToolDescriptions(allowedTools);
        String prompt = """
            你是 Agent 二次规划器。请根据已有观察决定是否补充工具调用。
            约束：
            1) 只能使用工具：
            %s
            2) 参数必须严格使用工具描述中定义的枚举值，不要使用中文别名
            3) 已调用过的工具：%s
            4) 最多返回 %d 个 toolCalls
            5) 只输出 JSON：{"toolCalls":[{"toolName":"...","args":{...}}],"done":true/false}
            路由类型：%s
            用户问题：%s
            现有观察：
            %s
            """.formatted(toolDescriptions, executedToolNames, remainingSteps, routePlan.routeType(), request.getMessage(),
            observations.isEmpty() ? "暂无观察" : String.join("\n", observations));
        try {
            String raw = agentLlmClient.call("REPLAN", prompt);
            List<ToolCall> planned = structuredOutputValidator.parseToolCalls(raw, allowedTools, remainingSteps);
            planned = agentPolicyEnforcer.enforcePolicyTools(request, routePlan, allowedTools, planned, executedToolNames, remainingSteps);
            return new AgentPlanResult(planned, raw, System.currentTimeMillis() - startedAt, false);
        } catch (Exception ex) {
            log.warn("Agent Replan 失败，跳过本轮: {}", ex.getMessage());
            return new AgentPlanResult(List.of(), "replan-fallback", System.currentTimeMillis() - startedAt, true);
        }
    }
}
