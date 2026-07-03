package com.example.aidevelop.model.rag;

/**
 * 统一 RAG 检索请求。
 */
public record RagRequest(
    String query,
    String conversationId,
    int topK,
    double similarityThreshold,
    RagProfile profile,
    String documentType
) {
    public RagRequest {
        topK = topK <= 0 ? 5 : topK;
        profile = profile == null ? RagProfile.CHAT : profile;
    }
}
