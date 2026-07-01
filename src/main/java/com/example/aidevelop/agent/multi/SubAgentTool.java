package com.example.aidevelop.agent.multi;

import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.agent.model.AgentResponse;
import com.example.aidevelop.agent.tool.AgentTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubAgentTool implements AgentTool {

    private final SubAgentRunner subAgentRunner;
    private final MultiAgentProperties multiAgentProperties;

    @Override
    public String name() {
        return multiAgentProperties.getAgentAsTool().getToolName();
    }

    @Override
    public Object execute(Map<String, Object> args) {
        if (!multiAgentProperties.isEnabled() || !multiAgentProperties.getAgentAsTool().isEnabled()) {
            return "多 Agent 功能未启用";
        }

        String agentName = readString(args, "agentName");
        String query = readString(args, "query");

        if (agentName == null || agentName.isBlank()) {
            return "缺少必填参数: agentName";
        }

        if (!multiAgentProperties.getAgentAsTool().getDelegatableAgents().contains(agentName)) {
            return "不允许委派的 Agent: " + agentName;
        }

        if (!multiAgentProperties.getAgents().containsKey(agentName)) {
            return "未定义的 Agent: " + agentName;
        }

        log.info("Agent-as-Tool 委派执行: agentName={}, query={}", agentName, query);

        SubAgentDefinition definition = multiAgentProperties.toDefinition(agentName);
        MultiAgentState sharedState = new MultiAgentState("delegate-" + System.currentTimeMillis());

        AgentRequest subRequest = new AgentRequest();
        subRequest.setMessage(query != null && !query.isBlank() ? query : "请执行你的职责");
        subRequest.setMaxSteps(definition.maxSteps());

        String context = readString(args, "context");
        if (context != null && !context.isBlank()) {
            sharedState.put("caller_context", context);
        }

        AgentResponse response = subAgentRunner.execute(subRequest, definition, sharedState);
        return response.getFinalAnswer();
    }

    private String readString(Map<String, Object> args, String key) {
        Object value = args.get(key);
        return value != null ? String.valueOf(value) : null;
    }
}
