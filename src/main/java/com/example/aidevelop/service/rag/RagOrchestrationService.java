package com.example.aidevelop.service.rag;

import com.example.aidevelop.config.RagProperties;
import com.example.aidevelop.model.dto.rag.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 编排服务接口。
 * 为 Controller 提供统一入口，隔离底层检索/重排/评估实现细节。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RagOrchestrationService {

    private final VectorStore vectorStore;
    private final RagProperties ragProperties;
    private final QueryExpansionService queryExpansionService;
    private final HybridSearchService hybridSearchService;
    private final RerankService rerankService;
    private final RagPipelineService ragPipelineService;
    private final RagEvaluationService ragEvaluationService;

    public List<SearchResultDTO> search(String query, String type, int topK) {
        log.info("知识库检索 - query: {}, type: {}, topK: {}", query, type, topK);

        int searchTopK = type != null && !type.isEmpty() ? topK * 5 : topK;
        String expandedQuery = queryExpansionService.expandQuery(query);
        log.info("查询扩展: {} -> {}", query, expandedQuery);

        SearchRequest searchRequest = SearchRequest.query(expandedQuery)
                .withTopK(searchTopK)
                .withSimilarityThreshold(ragProperties.getSimilarityThreshold());

        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        log.info("检索到 {} 个文档片段（过滤前）", documents.size());

        if (type != null && !type.isEmpty()) {
            documents = documents.stream()
                    .filter(doc -> {
                        Object docType = doc.getMetadata().get("type");
                        boolean matches = docType != null && docType.toString().equals(type);
                        log.debug("文档类型过滤: type={}, matches={}", docType, matches);
                        return matches;
                    })
                    .limit(topK)
                    .collect(Collectors.toList());
            log.info("类型 '{}' 过滤后剩余 {} 个文档", type, documents.size());
        }

        return documents.stream()
                .map(doc -> new SearchResultDTO(doc.getContent(), doc.getMetadata(), doc.getScore()))
                .collect(Collectors.toList());
    }

    public List<HybridSearchResultDTO> hybridSearch(String query, int topK) {
        log.info("混合检索 - query: {}, topK: {}", query, topK);
        var hybridResults = hybridSearchService.hybridSearch(query, topK);

        return hybridResults.stream()
                .map(result -> {
                    var source = HybridSearchResultDTO.determineSource(result.getVectorRank(), result.getBm25Rank());
                    return new HybridSearchResultDTO(
                            result.getDocument().getContent(),
                            result.getDocument().getMetadata(),
                            result.getFinalScore(),
                            result.getVectorRank(),
                            result.getBm25Rank(),
                            result.getVectorScore(),
                            result.getBm25Score(),
                            source);
                })
                .collect(Collectors.toList());
    }

    public List<RerankSearchResultDTO> rerankSearch(String query, int topK) {
        log.info("重排序检索 - query: {}, topK: {}", query, topK);
        var rerankResults = rerankService.rerankSearch(query, topK);

        return rerankResults.stream()
                .map(result -> new RerankSearchResultDTO(
                        result.document().getContent(),
                        result.document().getMetadata(),
                        result.rerankScore(),
                        result.getVectorScore(),
                        result.getScoreImprovement(),
                        result.reranked(),
                        formatRankChange(result.getVectorScore(), result.rerankScore())))
                .collect(Collectors.toList());
    }

    public PipelineSearchResultDTO pipelineSearch(String query, String conversationId, int topK) {
        log.info("RAG 管道检索 - query: {}, conversationId: {}, topK: {}", query, conversationId, topK);
        var pipelineResult = ragPipelineService.search(query, conversationId, topK);

        List<PipelineSearchResultDTO.DocumentResult> documentResults = pipelineResult.getDocuments().stream()
                .map(doc -> new PipelineSearchResultDTO.DocumentResult(doc.getContent(), doc.getMetadata(), doc.getScore()))
                .collect(Collectors.toList());

        return new PipelineSearchResultDTO(
                pipelineResult.getOriginalQuery(),
                pipelineResult.getRewrittenQuery(),
                pipelineResult.getExpandedQuery(),
                pipelineResult.getStrategy().toString(),
                pipelineResult.getTransformationSummary(),
                documentResults);
    }

    public EvaluationMetricsDTO evaluate(String query, List<String> relevantDocIds, int topK,
                                         double minTargetRecall, double minTargetPrecision) {
        log.info("RAG 系统评估 - query: {}, topK: {}", query, topK);
        var metrics = ragEvaluationService.evaluate(query, relevantDocIds, topK);

        return new EvaluationMetricsDTO(
                metrics.query(),
                metrics.retrievedCount(),
                metrics.relevantCount(),
                Math.round(metrics.recall() * 1000.0) / 1000.0,
                Math.round(metrics.precision() * 1000.0) / 1000.0,
                Math.round(metrics.f1() * 1000.0) / 1000.0,
                Math.round(metrics.mrr() * 1000.0) / 1000.0,
                Math.round(metrics.ndcg() * 1000.0) / 1000.0,
                metrics.meetsTarget(minTargetRecall, minTargetPrecision),
                metrics.detailedInfo());
    }

    private String formatRankChange(double vectorScore, double rerankScore) {
        double improvement = rerankScore - vectorScore;
        if (improvement > 0.2) {
            return String.format("显著提升（+%.2f）", improvement);
        } else if (improvement > 0) {
            return String.format("小幅提升（+%.2f）", improvement);
        } else if (improvement < -0.2) {
            return String.format("显著下降（%.2f）", improvement);
        } else if (improvement < 0) {
            return String.format("小幅下降（%.2f）", improvement);
        }
        return "基本持平";
    }
}
