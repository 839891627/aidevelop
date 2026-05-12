package com.example.aidevelop.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 对话消息持久化实体。
 */
@Data
@Entity
@Table(name = "chat_message", indexes = {
        @Index(name = "idx_conv_created", columnList = "conversation_id, created_at"),
        @Index(name = "idx_message_id", columnList = "message_id", unique = true)
})
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, length = 64, unique = true)
    private String messageId;

    @Column(name = "conversation_id", nullable = false, length = 64)
    private String conversationId;

    @Column(name = "role", nullable = false, length = 16)
    private String role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
