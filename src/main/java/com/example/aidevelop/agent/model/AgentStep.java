package com.example.aidevelop.agent.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Agent 执行步骤")
public class AgentStep {

    @Schema(description = "步骤序号", example = "1")
    private int stepIndex;

    @Schema(description = "步骤类型")
    private AgentActionType actionType;

    @Schema(description = "工具名称（非工具步骤为空）", example = "rag.search")
    private String toolName;

    @Schema(description = "工具输入参数")
    private Map<String, Object> toolInput;

    @Schema(description = "步骤输出摘要")
    private String toolOutput;

    @Schema(description = "步骤耗时（毫秒）", example = "120")
    private long latencyMs;

    @Schema(description = "步骤是否成功", example = "true")
    private boolean success;

    @Schema(description = "错误信息（成功时为空）")
    private String errorMessage;
}
