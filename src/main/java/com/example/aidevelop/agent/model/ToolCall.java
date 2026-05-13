package com.example.aidevelop.agent.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Agent 计划中的工具调用")
public record ToolCall(
    @Schema(description = "工具名称", example = "rag.search")
    String toolName,
    @Schema(description = "工具参数")
    Map<String, Object> args
) {
}
