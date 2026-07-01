package com.example.aidevelop.agent.multi;

import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.agent.model.AgentResponse;
import com.example.aidevelop.agent.service.AgentLoopService;
import com.example.aidevelop.agent.service.AgentService;
import com.example.aidevelop.service.IntentRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@Slf4j
@RequiredArgsConstructor
public class AgentDispatcher implements AgentService {

    private final AgentLoopService singleAgentService;
    private final SupervisorOrchestrator multiAgentService;
    private final MultiAgentProperties multiAgentProperties;
    private final IntentRoutingService intentRoutingService;

    @Override
    public AgentResponse chat(AgentRequest request) {
        if (shouldUseMultiAgent(request)) {
            log.info("路由到多 Agent 模式: message={}", request.getMessage());
            return multiAgentService.chat(request);
        }
        log.debug("路由到单 Agent 模式: message={}", request.getMessage());
        return singleAgentService.chat(request);
    }

    private boolean shouldUseMultiAgent(AgentRequest request) {
        if (!multiAgentProperties.isEnabled()) {
            return false;
        }

        if (Boolean.TRUE.equals(request.getMultiAgent())) {
            return true;
        }

        if (Boolean.FALSE.equals(request.getMultiAgent())) {
            return false;
        }

        IntentRoutingService.RoutePlan plan = intentRoutingService.plan(request.getMessage());
        return plan.routeType() == IntentRoutingService.RouteType.MULTI_AGENT;
    }
}
