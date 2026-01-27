package com.example.aidevelop.model.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * RAG 管道检索结果 DTO
 * 展示完整的 RAG 管道执行结果，包括查询转换过程和最终检索结果
 */
@Data
@AllArgsConstructor
@Schema(description = "RAG 管道检索结果")
public class PipelineSearchResultDTO {

    @Schema(description = "原始查询", example = "它怎么办")
    private String originalQuery;

    @Schema(description = "重写后的查询（如果未重写则与原始查询相同）", example = "产品A逾期了怎么办")
    private String rewrittenQuery;

    @Schema(description = "扩展后的查询（如果未扩展则与重写查询相同）", example = "产品A逾期 OR 拖欠 OR 违约了怎么办")
    private String expandedQuery;

    @Schema(description = "使用的检索策略", example = "VECTOR_WITH_RERANK")
    private String strategy;

    @Schema(description = "查询转换过程摘要")
    private String transformationSummary;

    @Schema(description = "检索到的文档列表")
    private java.util.List<DocumentResult> documents;

    /**
     * 文档结果
     */
    @Data
    @AllArgsConstructor
    public static class DocumentResult {
        @Schema(description = "文档内容")
        private String content;

        @Schema(description = "文档元数据")
        private Map<String, Object> metadata;

        @Schema(description = "相似度分数（如果有）")
        private Double score;
    }
}
