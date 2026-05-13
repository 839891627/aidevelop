package com.example.aidevelop.agent.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Agent 对话请求")
public class AgentRequest {

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "用户消息", example = "帮我先查一下借款记录，再说明相关风控规则", required = true)
    private String message;

    @Schema(description = "对话 ID（可选，用于查询改写）", example = "123e4567-e89b-12d3-a456-426614174000")
    @Size(max = 64, message = "conversationId 长度不能超过 64 个字符")
    private String conversationId;

    @Schema(description = "最大执行步数（可选，默认使用系统配置）", example = "3", minimum = "1", maximum = "10")
    @Min(value = 1, message = "maxSteps 不能小于 1")
    @Max(value = 10, message = "maxSteps 不能大于 10")
    private Integer maxSteps;
}
