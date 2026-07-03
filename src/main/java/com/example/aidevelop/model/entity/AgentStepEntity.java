package com.example.aidevelop.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Agent 执行步骤明细。
 */
@Data
@Entity
@Table(name = "agent_step", indexes = {
    @Index(name = "idx_agent_step_trace", columnList = "trace_id, step_index"),
    @Index(name = "idx_agent_step_action", columnList = "action_type")
})
public class AgentStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", nullable = false, length = 128)
    private String traceId;

    @Column(name = "step_index", nullable = false)
    private Integer stepIndex;

    @Column(name = "round_index")
    private Integer roundIndex;

    @Column(name = "action_type", length = 32, nullable = false)
    private String actionType;

    @Column(name = "step_status", length = 32)
    private String stepStatus;

    @Column(name = "failure_reason", length = 64)
    private String failureReason;

    @Column(name = "tool_name", length = 128)
    private String toolName;

    @Column(name = "tool_input_json", columnDefinition = "TEXT")
    private String toolInputJson;

    @Column(name = "tool_output", columnDefinition = "TEXT")
    private String toolOutput;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "success")
    private Boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
    }
}
