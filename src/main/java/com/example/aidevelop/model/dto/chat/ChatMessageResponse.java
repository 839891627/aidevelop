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
@Schema(description = "聊天历史消息")
public class ChatMessageResponse {

    @Schema(description = "消息 ID")
    private String messageId;

    @Schema(description = "消息角色：USER/ASSISTANT/SYSTEM", example = "USER")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "使用的模型名称")
    private String model;

    @Schema(description = "消息创建时间")
    private LocalDateTime createdAt;
}
