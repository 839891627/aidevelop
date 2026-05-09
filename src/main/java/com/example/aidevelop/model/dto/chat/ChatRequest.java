package com.example.aidevelop.model.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "聊天请求")
public class ChatRequest {

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "用户发送的消息内容", example = "你好，介绍一下 Spring AI", required = true)
    private String message;

    @Schema(description = "对话 ID（可选），用于多轮对话。首次对话不传，后续使用响应中返回的 conversationId", example = "123e4567-e89b-12d3-a456-426614174000")
    @Size(max = 64, message = "conversationId 长度不能超过 64 个字符")
    private String conversationId;

    @Schema(description = "指定使用的模型（可选），不传则使用配置中的默认模型", example = "deepseek-chat")
    @Size(max = 100, message = "model 长度不能超过 100 个字符")
    private String model;

    @Schema(description = "温度参数（可选），控制输出的随机性。范围 0.0-1.0，越高越随机", example = "0.7", minimum = "0", maximum = "1")
    @DecimalMin(value = "0.0", message = "temperature 不能小于 0")
    @DecimalMax(value = "1.0", message = "temperature 不能大于 1")
    private Double temperature;

    @Schema(description = "最大生成 token 数（可选）", example = "1000", minimum = "1", maximum = "4000")
    @Min(value = 1, message = "maxTokens 不能小于 1")
    @Max(value = 32000, message = "maxTokens 不能大于 32000")
    private Integer maxTokens;
}
