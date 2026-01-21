package com.example.aidevelop.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String conversationId;
    private String message;
    private String model;
    private Integer tokensUsed;
    private Long responseTime;  // 响应时间（毫秒）
}
