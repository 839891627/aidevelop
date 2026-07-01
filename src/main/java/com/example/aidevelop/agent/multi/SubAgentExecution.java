package com.example.aidevelop.agent.multi;

import com.example.aidevelop.agent.model.AgentResponse;

public record SubAgentExecution(
    String agentName,
    String outputKey,
    AgentResponse response,
    long latencyMs
) {}
