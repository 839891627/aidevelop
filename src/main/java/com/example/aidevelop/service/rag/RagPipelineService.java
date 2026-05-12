package com.example.aidevelop.service.rag;

import com.example.aidevelop.config.RagPipelineProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * 智能 RAG 管道服务
 *
 * 功能：根据查询特点自动组合使用多种 RAG 技巧，实现最佳的检索效果
 *
 * 管道流程：
 * 1. 查询重写（多轮对话、指代消解）
 * 2. 查询扩展（同义词、专业术语）
 * 3. 智能检索策略选择
 *    - 简单查询：向量检索
 *    - 专有名词：混合检索
 *    - 问题型查询：向量检索 + 重排序
 * 4. 返回检索结果
 */
@Service
@RequiredArgsConstructor
public class RagPipelineService {
    private static final Logger log = LoggerFactory.getLogger(RagPipelineService.class);

    private final QueryRewriteService queryRewriteService;
    private final QueryExpansionService queryExpansionService;
    private final HybridSearchService hybridSearchService;
    private final RerankService rerankService;
    private final VectorStore vectorStore;
    private final RagPipelineProperties pipelineProperties;

    /**
     * 智能 RAG 管道主方法
     *
     * @param originalQuery 原始查询
     * @param conversationId 对话ID（用于查询重写）
     * @param topK 返回数量
     * @return 检索结果
     */
    public PipelineResult search(String originalQuery, String conversationId, int topK) {
        log.info("=== RAG 管道开始 ===");
        log.info("原始查询: {}, conversationId: {}", originalQuery, conversationId);

        PipelineResult result = new PipelineResult();
        result.setOriginalQuery(originalQuery);

        // ========== 阶段 1: 查询重写 ==========
        String rewrittenQuery = rewriteQuery(originalQuery, conversationId);
        result.setRewrittenQuery(rewrittenQuery);

        // ========== 阶段 2: 查询扩展 ==========
        String expandedQuery = expandQuery(rewrittenQuery);
        result.setExpandedQuery(expandedQuery);

        // ========== 阶段 3: 选择检索策略 ==========
        SearchResultStrategy strategy = selectStrategy(expandedQuery);
        result.setStrategy(strategy);

        log.info("选择的检索策略: {}", strategy);

        // ========== 阶段 4: 执行检索 ==========
        List<Document> documents = executeSearch(expandedQuery, conversationId, strategy, topK);
        result.setDocuments(documents);

        log.info("=== RAG 管道完成 ===");
        log.info("最终检索到 {} 个文档", documents.size());

        return result;
    }

    /**
     * 阶段 1：查询重写
     */
    private String rewriteQuery(String query, String conversationId) {
        if (!pipelineProperties.isEnableQueryRewrite()) {
            log.debug("查询重写已禁用，跳过");
            return query;
        }

        String rewritten = queryRewriteService.rewriteQuery(query, conversationId);
        if (!rewritten.equals(query)) {
            log.info("✓ 查询重写: {} → {}", query, rewritten);
        } else {
            log.debug("查询无需重写");
        }

        return rewritten;
    }

    /**
     * 阶段 2：查询扩展
     */
    private String expandQuery(String query) {
        if (!pipelineProperties.isEnableQueryExpansion()) {
            log.debug("查询扩展已禁用，跳过");
            return query;
        }

        String expanded = queryExpansionService.expandQuery(query);
        if (!expanded.equals(query)) {
            log.info("✓ 查询扩展: {} → {}", query, expanded);
        } else {
            log.debug("查询无需扩展");
        }

        return expanded;
    }

    /**
     * 阶段 3：选择检索策略
     */
    private SearchResultStrategy selectStrategy(String query) {
        // 如果不是自动模式，使用固定策略
        if (!pipelineProperties.isAutoMode()) {
            return buildFixedStrategy();
        }

        // 自动模式：根据查询特点选择最佳策略
        log.debug("使用自动模式选择策略...");

        // 检测查询特点
        boolean isQuestionQuery = isQuestionQuery(query);
        boolean hasTechnicalTerms = hasTechnicalTerms(query);
        boolean isLongQuery = query.length() > pipelineProperties.getComplexQueryLength();

        // 策略选择逻辑
        if (isQuestionQuery) {
            // 问题型查询：启用重排序，提升准确率
            if (pipelineProperties.isEnableRerank()) {
                return SearchResultStrategy.VECTOR_WITH_RERANK;
            }
        }

        if (hasTechnicalTerms) {
            // 包含专有名词：使用混合检索
            if (pipelineProperties.isEnableHybridSearch()) {
                return SearchResultStrategy.HYBRID_SEARCH;
            }
        }

        if (isLongQuery) {
            // 复杂查询：使用混合检索 + 重排序
            if (pipelineProperties.isEnableHybridSearch() && pipelineProperties.isEnableRerank()) {
                return SearchResultStrategy.HYBRID_WITH_RERANK;
            } else if (pipelineProperties.isEnableHybridSearch()) {
                return SearchResultStrategy.HYBRID_SEARCH;
            } else if (pipelineProperties.isEnableRerank()) {
                return SearchResultStrategy.VECTOR_WITH_RERANK;
            }
        }

        // 默认策略：纯向量检索
        return SearchResultStrategy.VECTOR_ONLY;
    }

    /**
     * 阶段 4：执行检索
     */
    private List<Document> executeSearch(String query, String conversationId,
                                         SearchResultStrategy strategy, int topK) {
        log.info("执行策略: {}", strategy);

        return switch (strategy) {
            case VECTOR_ONLY -> {
                log.info("执行：向量检索");
                yield vectorSearch(query, topK);
            }
            case HYBRID_SEARCH -> {
                log.info("执行：混合检索（向量 + BM25）");
                var results = hybridSearchService.hybridSearch(query, topK);
                yield results.stream()
                    .map(HybridSearchService.HybridSearchResult::getDocument)
                    .toList();
            }
            case VECTOR_WITH_RERANK -> {
                log.info("执行：向量检索 + 重排序");
                var results = rerankService.rerankSearch(query, topK);
                yield results.stream()
                    .map(RerankService.RerankResult::document)
                    .toList();
            }
            case HYBRID_WITH_RERANK -> {
                log.info("执行：混合检索 + 重排序");
                // 先混合检索召回更多结果
                var hybridResults = hybridSearchService.hybridSearch(query, topK * 2);
                // 提取文档
                List<Document> candidates = hybridResults.stream()
                    .map(HybridSearchService.HybridSearchResult::getDocument)
                    .toList();
                // TODO: 这里可以进一步实现重排序，但目前直接返回混合检索结果
                yield candidates.stream().limit(topK).toList();
            }
        };
    }

    /**
     * 向量检索
     */
    private List<Document> vectorSearch(String query, int topK) {
        SearchRequest searchRequest = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .similarityThreshold(0.2)  // 固定阈值
            .build();

        return vectorStore.similaritySearch(searchRequest);
    }

    /**
     * 判断是否为问题型查询
     */
    private boolean isQuestionQuery(String query) {
        return Arrays.stream(pipelineProperties.getQuestionWords())
            .anyMatch(query::contains);
    }

    /**
     * 判断是否包含专有名词
     * 简单实现：检测大写字母、数字、特殊模式
     */
    private boolean hasTechnicalTerms(String query) {
        // 包含大写字母（如 M1, M2, VIP）
        boolean hasUpperCase = query.matches(".*[A-Z].*");

        // 包含数字（如 30天, 90天）
        boolean hasNumbers = query.matches(".*\\d+.*");

        // 包含特殊符号（如 CUST001）
        boolean hasSpecialChars = query.matches(".*[A-Z]+\\d+.*");

        return hasUpperCase || hasNumbers || hasSpecialChars;
    }

    /**
     * 构建固定策略（非自动模式）
     */
    private SearchResultStrategy buildFixedStrategy() {
        boolean useHybrid = pipelineProperties.isEnableHybridSearch();
        boolean useRerank = pipelineProperties.isEnableRerank();

        if (useHybrid && useRerank) {
            return SearchResultStrategy.HYBRID_WITH_RERANK;
        } else if (useHybrid) {
            return SearchResultStrategy.HYBRID_SEARCH;
        } else if (useRerank) {
            return SearchResultStrategy.VECTOR_WITH_RERANK;
        } else {
            return SearchResultStrategy.VECTOR_ONLY;
        }
    }

    /**
     * 检索结果策略枚举
     */
    public enum SearchResultStrategy {
        VECTOR_ONLY,           // 仅向量检索
        HYBRID_SEARCH,         // 混合检索（向量 + BM25）
        VECTOR_WITH_RERANK,    // 向量检索 + 重排序
        HYBRID_WITH_RERANK     // 混合检索 + 重排序
    }

    /**
     * 管道执行结果
     */
    public static class PipelineResult {
        private String originalQuery;
        private String rewrittenQuery;
        private String expandedQuery;
        private SearchResultStrategy strategy;
        private List<Document> documents;

        // Getters and Setters
        public String getOriginalQuery() { return originalQuery; }
        public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }

        public String getRewrittenQuery() { return rewrittenQuery; }
        public void setRewrittenQuery(String rewrittenQuery) { this.rewrittenQuery = rewrittenQuery; }

        public String getExpandedQuery() { return expandedQuery; }
        public void setExpandedQuery(String expandedQuery) { this.expandedQuery = expandedQuery; }

        public SearchResultStrategy getStrategy() { return strategy; }
        public void setStrategy(SearchResultStrategy strategy) { this.strategy = strategy; }

        public List<Document> getDocuments() { return documents; }
        public void setDocuments(List<Document> documents) { this.documents = documents; }

        /**
         * 获取查询转换摘要
         */
        public String getTransformationSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("原始查询: ").append(originalQuery).append("\n");

            if (!originalQuery.equals(rewrittenQuery)) {
                sb.append("  → 重写: ").append(rewrittenQuery).append("\n");
            }

            if (!rewrittenQuery.equals(expandedQuery)) {
                sb.append("  → 扩展: ").append(expandedQuery).append("\n");
            }

            sb.append("  → 策略: ").append(strategy).append("\n");
            sb.append("  → 结果数: ").append(documents != null ? documents.size() : 0);

            return sb.toString();
        }
    }
}
