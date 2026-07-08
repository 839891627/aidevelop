package com.example.aidevelop.eval;

import java.util.List;
import java.util.Map;

/**
 * 单条评估输出，既可作为报告明细，也可导出给外部评估工具复用。
 */
public record RagasEvalResult(
    String id,
    String question,
    String answer,
    List<String> contexts,
    String groundTruth,
    List<String> referenceContexts,
    RagasEvalScores scores,
    Map<String, Object> retrievalMetadata
) {
    public RagasEvalResult {
        contexts = contexts == null ? List.of() : List.copyOf(contexts);
        referenceContexts = referenceContexts == null ? List.of() : List.copyOf(referenceContexts);
        retrievalMetadata = retrievalMetadata == null ? Map.of() : Map.copyOf(retrievalMetadata);
    }
}
