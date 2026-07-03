package com.example.aidevelop.model.rag;

import java.util.List;

/**
 * 统一 RAG 检索结果，供 Chat 和 Agent 共享。
 */
public record RagRetrievalResult(
    String originalQuery,
    String rewrittenQuery,
    String expandedQuery,
    String strategy,
    String transformationSummary,
    List<RagDocumentResult> documents,
    boolean empty,
    boolean degraded,
    String reason
) {
    public static RagRetrievalResult disabled(String query, String reason) {
        return new RagRetrievalResult(query, query, query, "DISABLED", reason, List.of(), true, true, reason);
    }
}
