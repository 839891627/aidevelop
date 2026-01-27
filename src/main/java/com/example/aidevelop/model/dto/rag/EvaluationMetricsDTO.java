package com.example.aidevelop.model.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 评估指标 DTO
 * 展示单个查询的评估结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "RAG 评估指标")
public class EvaluationMetricsDTO {

    @Schema(description = "查询文本", example = "逾期处理流程")
    private String query;

    @Schema(description = "检索返回的文档数量", example = "5")
    private int retrievedCount;

    @Schema(description = "相关文档总数", example = "8")
    private int relevantCount;

    @Schema(description = "召回率（检索到的相关文档 / 所有相关文档）", example = "0.75")
    private double recall;

    @Schema(description = "精确率（检索到的相关文档 / 检索到的总文档）", example = "0.80")
    private double precision;

    @Schema(description = "F1分数（召回率和精确率的调和平均）", example = "0.77")
    private double f1;

    @Schema(description = "MRR（平均倒数排名）", example = "0.85")
    private double mrr;

    @Schema(description = "NDCG（归一化折损累计增益）", example = "0.82")
    private double ndcg;

    @Schema(description = "是否达到目标指标", example = "true")
    private boolean meetsTarget;

    @Schema(description = "详细信息")
    private String detailedInfo;
}
