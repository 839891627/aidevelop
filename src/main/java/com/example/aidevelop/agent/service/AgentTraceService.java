package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.agent.model.AgentResponse;
import com.example.aidevelop.agent.model.AgentStep;
import com.example.aidevelop.model.entity.AgentStepEntity;
import com.example.aidevelop.model.entity.AgentTraceEntity;
import com.example.aidevelop.repository.AgentStepRepository;
import com.example.aidevelop.repository.AgentTraceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentTraceService {

    private final AgentTraceRepository agentTraceRepository;
    private final AgentStepRepository agentStepRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public boolean persistTrace(AgentRequest request, AgentResponse response, String mode) {
        try {
            AgentTraceEntity trace = toTraceEntity(request, response, mode);
            agentTraceRepository.save(trace);
            List<AgentStepEntity> steps = response.getSteps() == null
                ? List.of()
                : response.getSteps().stream()
                    .map(step -> toStepEntity(response.getTraceId(), step))
                    .toList();
            agentStepRepository.saveAll(steps);
            return true;
        } catch (Exception ex) {
            log.warn("Agent trace 持久化失败: traceId={}, error={}", response.getTraceId(), ex.getMessage());
            return false;
        }
    }

    public AgentResponse findTrace(String traceId) {
        return agentTraceRepository.findByTraceId(traceId)
            .map(trace -> {
                List<AgentStep> steps = agentStepRepository.findByTraceIdOrderByStepIndexAsc(traceId)
                    .stream()
                    .map(this::toStep)
                    .toList();
                return AgentResponse.builder()
                    .traceId(trace.getTraceId())
                    .routeType(trace.getRouteType())
                    .status(trace.getStatus() == null ? null : com.example.aidevelop.agent.model.AgentStepStatus.valueOf(trace.getStatus()))
                    .failureReason(trace.getFailureReason() == null ? null : com.example.aidevelop.agent.model.AgentFailureReason.valueOf(trace.getFailureReason()))
                    .finalAnswer(trace.getFinalAnswer())
                    .completed(Boolean.TRUE.equals(trace.getCompleted()))
                    .executedSteps(trace.getExecutedSteps() == null ? 0 : trace.getExecutedSteps())
                    .responseTimeMs(trace.getResponseTimeMs() == null ? 0L : trace.getResponseTimeMs())
                    .tracePersisted(true)
                    .steps(steps)
                    .build();
            })
            .orElse(null);
    }

    private AgentTraceEntity toTraceEntity(AgentRequest request, AgentResponse response, String mode) {
        AgentTraceEntity entity = new AgentTraceEntity();
        entity.setTraceId(response.getTraceId());
        entity.setConversationId(request.getConversationId());
        entity.setRouteType(response.getRouteType());
        entity.setMode(mode);
        entity.setStatus(response.getStatus() == null ? null : response.getStatus().name());
        entity.setFailureReason(response.getFailureReason() == null ? null : response.getFailureReason().name());
        entity.setUserMessage(request.getMessage());
        entity.setFinalAnswer(response.getFinalAnswer());
        entity.setCompleted(response.isCompleted());
        entity.setExecutedSteps(response.getExecutedSteps());
        entity.setResponseTimeMs(response.getResponseTimeMs());
        return entity;
    }

    private AgentStepEntity toStepEntity(String traceId, AgentStep step) {
        AgentStepEntity entity = new AgentStepEntity();
        entity.setTraceId(traceId);
        entity.setStepIndex(step.getStepIndex());
        entity.setRoundIndex(step.getRoundIndex());
        entity.setActionType(step.getActionType() == null ? null : step.getActionType().name());
        entity.setStepStatus(step.getStatus() == null ? null : step.getStatus().name());
        entity.setFailureReason(step.getFailureReason() == null ? null : step.getFailureReason().name());
        entity.setToolName(step.getToolName());
        entity.setToolInputJson(toJson(step.getToolInput()));
        entity.setToolOutput(step.getToolOutput());
        entity.setLatencyMs(step.getLatencyMs());
        entity.setSuccess(step.isSuccess());
        entity.setErrorMessage(step.getErrorMessage());
        return entity;
    }

    private AgentStep toStep(AgentStepEntity entity) {
        return AgentStep.builder()
            .stepIndex(entity.getStepIndex() == null ? 0 : entity.getStepIndex())
            .roundIndex(entity.getRoundIndex() == null ? 0 : entity.getRoundIndex())
            .actionType(entity.getActionType() == null ? null : com.example.aidevelop.agent.model.AgentActionType.valueOf(entity.getActionType()))
            .status(entity.getStepStatus() == null ? null : com.example.aidevelop.agent.model.AgentStepStatus.valueOf(entity.getStepStatus()))
            .failureReason(entity.getFailureReason() == null ? null : com.example.aidevelop.agent.model.AgentFailureReason.valueOf(entity.getFailureReason()))
            .toolName(entity.getToolName())
            .toolOutput(entity.getToolOutput())
            .latencyMs(entity.getLatencyMs() == null ? 0L : entity.getLatencyMs())
            .success(Boolean.TRUE.equals(entity.getSuccess()))
            .errorMessage(entity.getErrorMessage())
            .build();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }
}
