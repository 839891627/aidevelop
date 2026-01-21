package com.example.aidevelop.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank(message = "消息内容不能为空")
    private String message;

    private String conversationId;  // 可选，用于继续对话

    private String model;  // 可选，指定模型

    private Double temperature;  // 可选，控制随机性

    private Integer maxTokens;  // 可选，最大生成长度
}
