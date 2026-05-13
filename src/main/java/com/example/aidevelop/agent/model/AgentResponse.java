package com.example.aidevelop.agent.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Agent 对话响应")
public class AgentResponse {

    @Schema(description = "本次执行链路追踪 ID", example = "f8635078-ed2d-4620-9e79-0f9315534fc5")
    private String traceId;

    @Schema(description = "路由类型", example = "HYBRID")
    private String routeType;

    @Schema(description = "最终回答")
    private String finalAnswer;

    @Schema(description = "是否正常完成", example = "true")
    private boolean completed;

    @Schema(description = "执行步数", example = "3")
    private int executedSteps;

    @Schema(description = "总耗时（毫秒）", example = "350")
    private long responseTimeMs;

    @Schema(description = "执行步骤详情")
    private List<AgentStep> steps;
}
