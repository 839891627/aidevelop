package com.example.aidevelop.model.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * 知识库检索结果 DTO
 * 用于展示向量库检索的文档片段及其相似度分数
 */
@Data
@AllArgsConstructor
@Schema(description = "知识库检索结果")
public class SearchResultDTO {

    @Schema(description = "文档内容片段", example = "逾期处理流程：...")
    private String content;

    @Schema(description = "文档元数据（类型、文件名等）")
    private Map<String, Object> metadata;

    @Schema(description = "相似度分数（0-1之间，越高越相似）", example = "0.85")
    private Double score;
}
