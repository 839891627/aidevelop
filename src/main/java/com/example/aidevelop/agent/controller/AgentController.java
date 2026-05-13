package com.example.aidevelop.agent.controller;

import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.agent.model.AgentResponse;
import com.example.aidevelop.agent.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Tag(name = "Agent Loop 接口", description = "Agent MVP：Plan -> Tool -> Respond")
public class AgentController {

    private final AgentService agentService;

    @PostMapping("/chat")
    @Operation(summary = "Agent 对话", description = "执行 Agent Loop MVP，返回 traceId、步骤明细与最终答案")
    public AgentResponse chat(@Valid @RequestBody AgentRequest request) {
        log.info("收到 Agent 请求: message={}", request.getMessage());
        return agentService.chat(request);
    }
}
