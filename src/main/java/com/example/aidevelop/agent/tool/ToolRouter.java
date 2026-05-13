package com.example.aidevelop.agent.tool;

import com.example.aidevelop.config.AgentProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ToolRouter {

    private final Map<String, AgentTool> toolRegistry;
    private final AgentProperties agentProperties;

    public ToolRouter(List<AgentTool> tools, AgentProperties agentProperties) {
        this.agentProperties = agentProperties;
        this.toolRegistry = new LinkedHashMap<>();
        for (AgentTool tool : tools) {
            this.toolRegistry.put(tool.name(), tool);
        }
    }

    public Object execute(String toolName, Map<String, Object> args) {
        if (!isAllowed(toolName)) {
            throw new IllegalArgumentException("工具未授权: " + toolName);
        }
        AgentTool tool = toolRegistry.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("工具不存在: " + toolName);
        }
        return tool.execute(args);
    }

    public boolean exists(String toolName) {
        return toolRegistry.containsKey(toolName);
    }

    public Set<String> allToolNames() {
        return toolRegistry.keySet();
    }

    private boolean isAllowed(String toolName) {
        List<String> allowedTools = agentProperties.getAllowedTools();
        return allowedTools == null || allowedTools.isEmpty() || allowedTools.contains(toolName);
    }
}
