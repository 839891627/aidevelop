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
 * Agent 执行 trace 头表。
 */
@Data
@Entity
@Table(name = "agent_trace", indexes = {
    @Index(name = "idx_agent_trace_id", columnList = "trace_id", unique = true),
    @Index(name = "idx_agent_trace_conversation", columnList = "conversation_id, created_time"),
    @Index(name = "idx_agent_trace_created", columnList = "created_time")
})
public class AgentTraceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", nullable = false, length = 128, unique = true)
    private String traceId;

    @Column(name = "conversation_id", length = 64)
    private String conversationId;

    @Column(name = "route_type", length = 32)
    private String routeType;

    @Column(name = "mode", length = 32)
    private String mode;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "failure_reason", length = 64)
    private String failureReason;

    @Column(name = "user_message", columnDefinition = "TEXT")
    private String userMessage;

    @Column(name = "final_answer", columnDefinition = "TEXT")
    private String finalAnswer;

    @Column(name = "completed")
    private Boolean completed;

    @Column(name = "executed_steps")
    private Integer executedSteps;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
    }
}
