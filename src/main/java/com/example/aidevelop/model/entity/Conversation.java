package com.example.aidevelop.model.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class Conversation {
    private String conversationId;
    private List<Message> messages = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int maxHistorySize = 10;

    /**
     * 添加消息到对话历史
     * 实现滑动窗口机制：保留 SYSTEM 消息，限制 USER/ASSISTANT 消息数量
     */
    public void addMessage(Message message) {
        messages.add(message);
        // 保持历史记录在限定范围内（保留 SYSTEM 消息）
        while (messages.stream().filter(m -> m.getRole() != MessageRole.SYSTEM).count()
                > maxHistorySize) {
            messages.stream()
                .filter(m -> m.getRole() != MessageRole.SYSTEM)
                .findFirst()
                .ifPresent(messages::remove);
        }
        this.updatedAt = LocalDateTime.now();
    }
}
