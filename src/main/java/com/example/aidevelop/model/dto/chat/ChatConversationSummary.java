package com.example.aidevelop.model.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "聊天会话摘要")
public class ChatConversationSummary {

    @Schema(description = "对话 ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private String conversationId;

    @Schema(description = "会话标题，默认取首条用户消息", example = "请查询 USER001 的借款记录")
    private String title;

    @Schema(description = "消息数量", example = "6")
    private Integer messageCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "最近更新时间")
    private LocalDateTime updatedAt;
}
