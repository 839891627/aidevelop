package com.example.aidevelop.agent.controller;

import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.agent.model.AgentResponse;
import com.example.aidevelop.agent.service.AgentService;
import com.example.aidevelop.agent.service.AgentTraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final AgentTraceService agentTraceService;

    @PostMapping("/chat")
    @Operation(summary = "Agent 对话", description = "执行 Agent Loop MVP，返回 traceId、步骤明细与最终答案")
    public AgentResponse chat(@Valid @RequestBody AgentRequest request) {
        log.info("收到 Agent 请求: message={}", request.getMessage());
        return agentService.chat(request);
    }

    @GetMapping("/trace/{traceId}")
    @Operation(summary = "查询 Agent 执行 Trace", description = "根据 traceId 返回已持久化的 Agent 执行步骤")
    public AgentResponse getTrace(@PathVariable String traceId) {
        return agentTraceService.findTrace(traceId);
    }
}
