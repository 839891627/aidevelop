package com.example.aidevelop.model.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "聊天响应")
public class ChatResponse {

    @Schema(description = "对话 ID，后续对话时需要传入此 ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private String conversationId;

    @Schema(description = "AI 返回的消息内容", example = "Spring AI 是 Spring 官方推出的 AI 应用开发框架...")
    private String message;

    @Schema(description = "使用的模型名称", example = "deepseek-v4-flash")
    private String model;

    @Schema(description = "本次对话消耗的 token 数", example = "256")
    private Integer tokensUsed;

    @Schema(description = "响应时间（毫秒）", example = "1234")
    private Long responseTime;
}
