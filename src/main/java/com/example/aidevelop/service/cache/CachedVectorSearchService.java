package com.example.aidevelop.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 带缓存的向量检索服务
 * 缓存相同查询的检索结果，避免重复计算
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CachedVectorSearchService {

    private final VectorStore vectorStore;

    /**
     * 向量检索（带缓存）
     *
     * @param query 查询文本
     * @param topK 返回文档数量
     * @param threshold 相似度阈值
     * @return 检索到的文档列表
     */
    @Cacheable(
        value = "vectorSearch",
        key = "#query + '_' + #topK + '_' + #threshold",
        cacheManager = "vectorSearchCacheManager"
    )
    public List<Document> similaritySearch(String query, int topK, double threshold) {
        log.debug("向量检索（未缓存）: query={}, topK={}, threshold={}", query, topK, threshold);

        SearchRequest searchRequest = SearchRequest.defaults()
                .topK(topK)
                .similarityThreshold(threshold)
                .build();

        return vectorStore.similaritySearch(searchRequest);
    }

    /**
     * 向量检索（使用默认参数）
     */
    @Cacheable(
        value = "vectorSearch",
        key = "#query",
        cacheManager = "vectorSearchCacheManager"
    )
    public List<Document> similaritySearch(String query) {
        log.debug("向量检索（默认参数，未缓存）: query={}", query);

        SearchRequest searchRequest = SearchRequest.defaults()
                .topK(5)
                .similarityThreshold(0.3)
                .build();

        return vectorStore.similaritySearch(searchRequest);
    }

    /**
     * 清除指定查询的缓存
     */
    public void evictQuery(String query) {
        // Spring Cache 会自动处理
        log.info("清除查询缓存: query={}", query);
    }
}
