package com.example.aidevelop.agent.tool;

import com.example.aidevelop.config.AgentProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ToolRouter {

    private final ObjectProvider<AgentTool> toolProvider;
    private volatile Map<String, AgentTool> toolRegistry;
    private final AgentProperties agentProperties;

    @Autowired
    public ToolRouter(ObjectProvider<AgentTool> toolProvider, AgentProperties agentProperties) {
        this.toolProvider = toolProvider;
        this.agentProperties = agentProperties;
    }

    public ToolRouter(List<AgentTool> tools, AgentProperties agentProperties) {
        this.toolProvider = null;
        this.agentProperties = agentProperties;
        this.toolRegistry = buildRegistry(tools);
    }

    public Object execute(String toolName, Map<String, Object> args) {
        if (!isAllowed(toolName)) {
            // 全局工具白名单兜底，防止模型或规划阶段生成未授权工具名。
            throw new IllegalArgumentException("工具未授权: " + toolName);
        }
        AgentTool tool = registry().get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("工具不存在: " + toolName);
        }
        return tool.execute(args);
    }

    public boolean exists(String toolName) {
        return registry().containsKey(toolName);
    }

    public Set<String> allToolNames() {
        return registry().keySet();
    }

    private boolean isAllowed(String toolName) {
        List<String> allowedTools = agentProperties.getAllowedTools();
        return allowedTools == null || allowedTools.isEmpty() || allowedTools.contains(toolName);
    }

    private Map<String, AgentTool> registry() {
        Map<String, AgentTool> localRegistry = toolRegistry;
        if (localRegistry == null) {
            synchronized (this) {
                localRegistry = toolRegistry;
                if (localRegistry == null) {
                    // 延迟构建工具注册表，避免启动时过早初始化所有 AgentTool。
                    localRegistry = buildRegistry(toolProvider.orderedStream().toList());
                    toolRegistry = localRegistry;
                }
            }
        }
        return localRegistry;
    }

    private Map<String, AgentTool> buildRegistry(List<AgentTool> tools) {
        Map<String, AgentTool> registry = new LinkedHashMap<>();
        for (AgentTool tool : tools) {
            registry.put(tool.name(), tool);
        }
        return registry;
    }
}
