package com.example.aidevelop.model.rag;

import java.util.Map;

/**
 * RAG 检索命中的文档片段。
 */
public record RagDocumentResult(
    String content,
    Map<String, Object> metadata,
    Double score
) {
}
