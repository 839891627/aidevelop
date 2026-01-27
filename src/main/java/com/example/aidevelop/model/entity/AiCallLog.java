package com.example.aidevelop.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 调用日志实体
 */
@Data
@Entity
@Table(name = "ai_call_log", indexes = {
    @Index(name = "idx_session", columnList = "session_id"),
    @Index(name = "idx_user", columnList = "user_id"),
    @Index(name = "idx_model", columnList = "model_name"),
    @Index(name = "idx_created", columnList = "created_time"),
    @Index(name = "idx_provider", columnList = "provider")
})
public class AiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 会话ID
     */
    @Column(name = "session_id", length = 64)
    private String sessionId;

    /**
     * 用户ID
     */
    @Column(name = "user_id", length = 64)
    private String userId;

    /**
     * 模型名称
     */
    @Column(name = "model_name", length = 50, nullable = false)
    private String modelName;

    /**
     * 模型类型: CHAT, EMBEDDING
     */
    @Column(name = "model_type", length = 20, nullable = false)
    private String modelType;

    /**
     * 提供商: OPENAI, ZHIPUAI
     */
    @Column(name = "provider", length = 20, nullable = false)
    private String provider;

    /**
     * 输入Token数
     */
    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    /**
     * 输出Token数
     */
    @Column(name = "completion_tokens")
    private Integer completionTokens;

    /**
     * 总Token数
     */
    @Column(name = "total_tokens")
    private Integer totalTokens;

    /**
     * 本次调用成本（元）
     */
    @Column(name = "cost", precision = 10, scale = 6)
    private BigDecimal cost;

    /**
     * 响应耗时（毫秒）
     */
    @Column(name = "latency_ms")
    private Long latencyMs;

    /**
     * 状态: SUCCESS, FAILURE, TIMEOUT
     */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 创建时间
     */
    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
    }
}
