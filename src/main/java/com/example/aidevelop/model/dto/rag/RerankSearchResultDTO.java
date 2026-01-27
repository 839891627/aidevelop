package com.example.aidevelop.model.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * 重排序检索结果 DTO
 * 展示 LLM 重排序后的检索结果
 */
@Data
@AllArgsConstructor
@Schema(description = "重排序检索结果")
public class RerankSearchResultDTO {

    @Schema(description = "文档内容片段")
    private String content;

    @Schema(description = "文档元数据（类型、文件名等）")
    private Map<String, Object> metadata;

    @Schema(description = "LLM 重排序后的分数（0-1之间）", example = "0.91")
    private double rerankScore;

    @Schema(description = "向量检索的原始分数（0-1之间）", example = "0.72")
    private double vectorScore;

    @Schema(description = "分数提升幅度", example = "0.19")
    private double scoreImprovement;

    @Schema(description = "是否经过 LLM 重排序（false表示降级为向量检索）", example = "true")
    private boolean reranked;

    @Schema(description = "排名变化", example = "从第5名提升到第1名")
    private String rankChange;
}
