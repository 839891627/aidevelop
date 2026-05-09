package com.example.aidevelop.controller;

import com.example.aidevelop.model.dto.rag.EvaluationMetricsDTO;
import com.example.aidevelop.model.dto.rag.HybridSearchResultDTO;
import com.example.aidevelop.model.dto.rag.PipelineSearchResultDTO;
import com.example.aidevelop.model.dto.rag.RerankSearchResultDTO;
import com.example.aidevelop.model.dto.rag.SearchResultDTO;
import com.example.aidevelop.service.rag.RagOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Tag(name = "RAG检索接口", description = "高级 RAG 检索链路：向量检索、混合检索、重排与评估")
public class RagController {

    private final RagOrchestrationService ragOrchestrationService;

    @GetMapping("/search")
    @Operation(summary = "知识库检索", description = "在向量库中检索相关文档，支持按类型过滤（规则/产品/风控）。该接口属于高级 RAG 能力，与 /api/chat 的内置 Advisor 链路不同。")
    public List<SearchResultDTO> search(
        @Parameter(description = "检索关键词", required = true, example = "逾期处理") @RequestParam @NotBlank @Size(max = 500) String query,
        @Parameter(description = "文档类型（规则/产品/风控）") @RequestParam(required = false) String type,
        @Parameter(description = "返回数量", example = "5") @RequestParam(defaultValue = "5") @Min(1) @Max(20) int topK) {
        return ragOrchestrationService.search(query, type, topK);
    }

    @GetMapping("/hybrid-search")
    @Operation(summary = "混合检索", description = "结合向量检索和BM25检索，使用RRF算法融合结果")
    public List<HybridSearchResultDTO> hybridSearch(
        @Parameter(description = "检索关键词", required = true, example = "M1阶段的征信上报") @RequestParam @NotBlank @Size(max = 500) String query,
        @Parameter(description = "返回数量", example = "5") @RequestParam(defaultValue = "5") @Min(1) @Max(20) int topK) {
        return ragOrchestrationService.hybridSearch(query, topK);
    }

    @GetMapping("/rerank-search")
    @Operation(summary = "重排序检索", description = "向量召回后使用LLM重排序，返回Top-K结果")
    public List<RerankSearchResultDTO> rerankSearch(
        @Parameter(description = "检索关键词", required = true, example = "客户逾期了怎么办") @RequestParam @NotBlank @Size(max = 500) String query,
        @Parameter(description = "返回数量", example = "5") @RequestParam(defaultValue = "5") @Min(1) @Max(20) int topK) {
        return ragOrchestrationService.rerankSearch(query, topK);
    }

    @GetMapping("/pipeline")
    @Operation(summary = "智能 RAG 管道检索", description = "自动组合查询重写、扩展、混合检索和重排序")
    public PipelineSearchResultDTO pipelineSearch(
        @Parameter(description = "检索关键词", required = true, example = "客户逾期了怎么办") @RequestParam @NotBlank @Size(max = 500) String query,
        @Parameter(description = "对话 ID（可选），用于查询重写") @RequestParam(required = false) String conversationId,
        @Parameter(description = "返回数量", example = "5") @RequestParam(defaultValue = "5") @Min(1) @Max(20) int topK) {
        return ragOrchestrationService.pipelineSearch(query, conversationId, topK);
    }

    @PostMapping("/evaluate")
    @Operation(summary = "RAG 系统评估", description = "评估 RAG 检索效果，计算召回率、精确率、F1、MRR、NDCG")
    public EvaluationMetricsDTO evaluate(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "评估请求体", required = true)
        @RequestBody EvaluationRequestDTO request) {
        return ragOrchestrationService.evaluate(
            request.getQuery(),
            request.getRelevantDocIds(),
            request.getTopK(),
            request.getMinTargetRecall(),
            request.getMinTargetPrecision());
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(name = "EvaluationRequest", description = "RAG 评估请求")
    public static class EvaluationRequestDTO {
        @Schema(description = "查询文本", example = "逾期处理流程", requiredMode = Schema.RequiredMode.REQUIRED)
        private String query;

        @Schema(description = "相关文档ID列表（应该被检索到的文档）", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<String> relevantDocIds;

        @Schema(description = "检索返回的文档数量", example = "5")
        private int topK = 5;

        @Schema(description = "目标最低召回率", example = "0.8")
        private double minTargetRecall = 0.7;

        @Schema(description = "目标最低精确率", example = "0.75")
        private double minTargetPrecision = 0.75;
    }
}

