package com.example.aidevelop.eval;

import java.util.List;

/**
 * RAG 离线评测集中的单条黄金样本。
 */
public record RagasEvalSample(
    String id,
    String question,
    String groundTruth,
    List<String> referenceContexts
) {
    public RagasEvalSample {
        referenceContexts = referenceContexts == null ? List.of() : List.copyOf(referenceContexts);
    }
}
