package com.example.aidevelop.model.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 查询扩展详情 DTO
 * 用于展示查询扩展的结果，包括原始查询、扩展后的查询以及扩展的详细信息
 */
@Data
@AllArgsConstructor
@Schema(description = "查询扩展详情")
public class QueryExpansionDetailDTO {

    @Schema(description = "原始查询", example = "黑名单")
    private String originalQuery;

    @Schema(description = "扩展后的查询（使用 OR 连接）", example = "黑名单 OR 征信黑名单 OR 不良记录")
    private String expandedQuery;

    @Schema(description = "同义词扩展详情", example = "[\"黑名单 → [征信黑名单, 不良记录]\"]")
    private List<String> synonymExpansions;

    @Schema(description = "专业术语扩展详情", example = "[\"M1 → [第一阶段, 1-30天]\"]")
    private List<String> technicalExpansions;
}
