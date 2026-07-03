package com.example.aidevelop.model.rag;

/**
 * RAG 调用场景，用于在统一 Facade 内保留不同入口的治理语义。
 */
public enum RagProfile {
    CHAT,
    AGENT,
    DEBUG
}
