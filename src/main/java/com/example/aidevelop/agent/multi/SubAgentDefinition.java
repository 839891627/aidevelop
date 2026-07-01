package com.example.aidevelop.agent.multi;

import java.util.List;

public record SubAgentDefinition(
    String name,
    String description,
    String systemPrompt,
    List<String> allowedTools,
    int maxSteps,
    boolean reflectEnabled,
    boolean replanEnabled,
    boolean selfCheckEnabled,
    double temperature,
    int maxTokens,
    String outputKey
) {}
