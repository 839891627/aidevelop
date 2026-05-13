package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.agent.model.AgentResponse;

public interface AgentService {

    AgentResponse chat(AgentRequest request);
}
