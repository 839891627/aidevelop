package com.example.aidevelop.agent.multi;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "app.chat.multi-agent")
public class MultiAgentProperties {

    private boolean enabled = false;
    private int maxSupervisorRounds = 5;
    private int supervisorTimeoutMs = 60000;
    private String supervisorSystemPrompt;
    private List<String> multiAgentKeywords = List.of();
    private boolean requireMultipleIntents = true;
    private Map<String, SubAgentConfig> agents = new LinkedHashMap<>();
    private AgentAsToolConfig agentAsTool = new AgentAsToolConfig();

    @Data
    public static class SubAgentConfig {
        private String description = "";
        private String systemPrompt = "";
        private List<String> allowedTools = List.of();
        private int maxSteps = 2;
        private boolean reflectEnabled = true;
        private boolean replanEnabled = false;
        private boolean selfCheckEnabled = false;
        private double temperature = 0.7;
        private int maxTokens = 1000;
        private String outputKey;
    }

    @Data
    public static class AgentAsToolConfig {
        private boolean enabled = true;
        private String toolName = "agent.delegate";
        private List<String> delegatableAgents = List.of();
    }

    public SubAgentDefinition toDefinition(String agentName) {
        SubAgentConfig config = agents.get(agentName);
        if (config == null) {
            throw new IllegalArgumentException("未找到子 Agent 定义: " + agentName);
        }
        return new SubAgentDefinition(
            agentName,
            config.getDescription(),
            config.getSystemPrompt(),
            config.getAllowedTools(),
            config.getMaxSteps(),
            config.isReflectEnabled(),
            config.isReplanEnabled(),
            config.isSelfCheckEnabled(),
            config.getTemperature(),
            config.getMaxTokens(),
            config.getOutputKey()
        );
    }
}
