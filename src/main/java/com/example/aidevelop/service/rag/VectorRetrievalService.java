package com.example.aidevelop.service.rag;

import com.example.aidevelop.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 统一向量检索入口，集中管理 SearchRequest 的默认参数。
 */
@Service
@RequiredArgsConstructor
public class VectorRetrievalService {

    private final VectorStore vectorStore;
    private final RagProperties ragProperties;

    public List<Document> search(String query) {
        return search(query, ragProperties.getTopK(), ragProperties.getSimilarityThreshold());
    }

    public List<Document> search(String query, int topK) {
        return search(query, topK, ragProperties.getSimilarityThreshold());
    }

    public List<Document> search(String query, int topK, double similarityThreshold) {
        SearchRequest searchRequest = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .similarityThreshold(similarityThreshold)
            .build();
        return vectorStore.similaritySearch(searchRequest);
    }
}
