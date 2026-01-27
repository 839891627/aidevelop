package com.example.aidevelop.model.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * 混合检索结果 DTO
 * 展示向量检索和BM25检索融合后的结果
 */
@Data
@AllArgsConstructor
@Schema(description = "混合检索结果")
public class HybridSearchResultDTO {

    @Schema(description = "文档内容片段")
    private String content;

    @Schema(description = "文档元数据（类型、文件名等）")
    private Map<String, Object> metadata;

    @Schema(description = "RRF融合后的最终分数", example = "0.0325")
    private double finalScore;

    @Schema(description = "在向量检索中的排名（null表示未在向量检索结果中）", example = "1")
    private Integer vectorRank;

    @Schema(description = "在BM25检索中的排名（null表示未在BM25检索结果中）", example = "2")
    private Integer bm25Rank;

    @Schema(description = "向量检索的相似度分数（0-1之间）", example = "0.85")
    private Double vectorScore;

    @Schema(description = "BM25检索的关键词匹配分数", example = "8.5")
    private double bm25Score;

    @Schema(description = "检索来源标识", example = "BOTH")
    private SearchSource source;

    /**
     * 检索来源枚举
     */
    public enum SearchSource {
        @Schema(description = "仅向量检索")
        VECTOR_ONLY,

        @Schema(description = "仅BM25检索")
        BM25_ONLY,

        @Schema(description = "两种检索都包含")
        BOTH
    }

    /**
     * 根据排名判断检索来源
     */
    public static SearchSource determineSource(Integer vectorRank, Integer bm25Rank) {
        boolean hasVector = vectorRank != null;
        boolean hasBm25 = bm25Rank != null;

        if (hasVector && hasBm25) {
            return SearchSource.BOTH;
        } else if (hasVector) {
            return SearchSource.VECTOR_ONLY;
        } else {
            return SearchSource.BM25_ONLY;
        }
    }
}
