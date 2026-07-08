package com.example.aidevelop.eval;

import com.example.aidevelop.model.rag.RagDocumentResult;
import com.example.aidevelop.model.rag.RagProfile;
import com.example.aidevelop.model.rag.RagRequest;
import com.example.aidevelop.model.rag.RagRetrievalResult;
import com.example.aidevelop.service.rag.RagFacade;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 将生产 RAG Facade 的检索结果转换为评估样本输出。
 */
public class RagasEvaluationExporter {

    private final Function<RagRequest, RagRetrievalResult> retriever;
    private final RagasLikeMetricCalculator metricCalculator;

    public RagasEvaluationExporter(RagFacade ragFacade, RagasLikeMetricCalculator metricCalculator) {
        this(ragFacade::retrieve, metricCalculator);
    }

    RagasEvaluationExporter(Function<RagRequest, RagRetrievalResult> retriever,
                            RagasLikeMetricCalculator metricCalculator) {
        this.retriever = retriever;
        this.metricCalculator = metricCalculator;
    }

    public RagasEvalResult export(RagasEvalSample sample, String answer, int topK, double similarityThreshold) {
        return export(sample, topK, similarityThreshold, ignored -> answer);
    }

    public RagasEvalResult export(RagasEvalSample sample,
                                  int topK,
                                  double similarityThreshold,
                                  Function<RagRetrievalResult, String> answerGenerator) {
        RagRetrievalResult retrievalResult = retriever.apply(new RagRequest(
            sample.question(),
            null,
            topK,
            similarityThreshold,
            RagProfile.DEBUG,
            null
        ));
        String answer = answerGenerator.apply(retrievalResult);
        List<String> contexts = retrievalResult.documents().stream()
            .map(RagDocumentResult::content)
            .toList();
        RagasEvalScores scores = metricCalculator.score(sample, answer, contexts);

        return new RagasEvalResult(
            sample.id(),
            sample.question(),
            answer,
            contexts,
            sample.groundTruth(),
            sample.referenceContexts(),
            scores,
            retrievalMetadata(retrievalResult)
        );
    }

    private Map<String, Object> retrievalMetadata(RagRetrievalResult result) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("original_query", result.originalQuery());
        metadata.put("rewritten_query", result.rewrittenQuery());
        metadata.put("expanded_query", result.expandedQuery());
        metadata.put("strategy", result.strategy());
        metadata.put("transformation_summary", result.transformationSummary());
        metadata.put("empty", result.empty());
        metadata.put("degraded", result.degraded());
        metadata.put("reason", result.reason());
        metadata.put("document_scores", result.documents().stream()
            .map(RagDocumentResult::score)
            .toList());
        return metadata;
    }
}
