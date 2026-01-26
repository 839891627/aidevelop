package com.example.aidevelop.model.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 查询重写详情 DTO
 * 用于展示查询重写的结果，包括原始查询、重写后的查询以及重写原因
 */
@Data
@AllArgsConstructor
@Schema(description = "查询重写详情")
public class QueryRewriteDetailDTO {

    @Schema(description = "原始查询", example = "它支持提前还款吗")
    private String originalQuery;

    @Schema(description = "重写后的查询", example = "产品A是否支持提前还款？")
    private String rewrittenQuery;

    @Schema(description = "是否发生了重写", example = "true")
    private Boolean changed;

    @Schema(description = "重写原因（如果未重写则为 null）", example = "指代消解：将代词替换为具体实体")
    private String reason;
}
