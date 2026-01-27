package com.example.aidevelop.service.rag;

import com.example.aidevelop.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索服务
 *
 * 结合向量检索（语义理解）和 BM25 检索（关键词匹配）的优势
 * 使用 RRF (Reciprocal Rank Fusion) 算法融合两种检索结果
 *
 * RRF 算法原理：
 * 1. 对每种检索结果按分数排序，得到排名
 * 2. 对每个文档的排名取倒数：1 / (k + rank)
 * 3. 将不同检索方法的倒数分数相加，得到最终分数
 *
 * 公式：
 * final_score(doc) = Σ 1 / (k + rank_i(doc))
 *
 * 其中：
 * - k: 平滑参数（通常取60），防止排名过高时分数过大
 * - rank_i(doc): 文档doc在第i种检索方法中的排名
 *
 * 优势：
 * - 不需要对不同检索方法的分数进行归一化
 * - 对异常值不敏感
 * - 计算简单，效果好
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HybridSearchService {

    private final VectorStore vectorStore;
    private final BM25Service bm25Service;
    private final RagProperties ragProperties;

    // RRF 平滑参数
    private static final int RRF_K = 60;

    /**
     * 混合检索主方法
     *
     * @param query 查询文本
     * @param topK 返回前K个结果
     * @return 融合后的检索结果
     */
    public List<HybridSearchResult> hybridSearch(String query, int topK) {
        log.info("开始混合检索 - query: '{}', topK: {}", query, topK);

        // 1. 并行执行两种检索
        List<Document> vectorResults = vectorSearch(query, topK);
        List<BM25Service.BM25Result> bm25Results = bm25Search(query, topK);

        log.debug("向量检索返回 {} 个结果，BM25检索返回 {} 个结果",
            vectorResults.size(), bm25Results.size());

        // 2. 使用 RRF 算法融合结果
        List<HybridSearchResult> mergedResults = reciprocalRankFusion(
            vectorResults,
            bm25Results,
            topK
        );

        log.info("混合检索完成，返回 {} 个融合结果", mergedResults.size());

        // 3. 打印前3个结果的详细信息（调试用）
        if (log.isDebugEnabled()) {
            mergedResults.stream().limit(3).forEach(result ->
                log.debug("融合结果 - score: {}, vector_rank: {}, bm25_rank: {}, content: {}",
                    result.getFinalScore(),
                    result.getVectorRank(),
                    result.getBm25Rank(),
                    result.getDocument().getContent().substring(0, Math.min(50, result.getDocument().getContent().length()))
                )
            );
        }

        return mergedResults;
    }

    /**
     * 向量检索（语义理解）
     */
    private List<Document> vectorSearch(String query, int topK) {
        try {
            SearchRequest searchRequest = SearchRequest.query(query)
                .withTopK(topK)
                .withSimilarityThreshold(ragProperties.getSimilarityThreshold());

            List<Document> results = vectorStore.similaritySearch(searchRequest);

            log.debug("向量检索完成，返回 {} 个结果", results.size());

            return results;
        } catch (Exception e) {
            log.error("向量检索失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * BM25 检索（关键词匹配）
     */
    private List<BM25Service.BM25Result> bm25Search(String query, int topK) {
        try {
            List<BM25Service.BM25Result> results = bm25Service.search(query, topK);

            log.debug("BM25检索完成，返回 {} 个结果", results.size());

            return results;
        } catch (Exception e) {
            log.error("BM25检索失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * RRF (Reciprocal Rank Fusion) 融合算法
     *
     * 算法步骤：
     * 1. 为每种检索方法的结果建立排名索引
     * 2. 对每个文档计算 RRF 分数：1 / (k + rank)
     * 3. 将不同检索方法的 RRF 分数相加
     * 4. 按最终分数排序，返回 Top-K
     *
     * @param vectorResults 向量检索结果
     * @param bm25Results BM25检索结果
     * @param topK 返回前K个结果
     * @return 融合后的结果
     */
    private List<HybridSearchResult> reciprocalRankFusion(
        List<Document> vectorResults,
        List<BM25Service.BM25Result> bm25Results,
        int topK
    ) {
        // 1. 建立文档ID到排名的映射
        Map<String, Integer> vectorRanks = new HashMap<>();
        Map<String, Document> vectorDocs = new HashMap<>();

        for (int i = 0; i < vectorResults.size(); i++) {
            Document doc = vectorResults.get(i);
            String docId = getDocumentId(doc);
            vectorRanks.put(docId, i + 1);  // 排名从1开始
            vectorDocs.put(docId, doc);
        }

        Map<String, Integer> bm25Ranks = new HashMap<>();
        Map<String, Double> bm25Scores = new HashMap<>();

        for (int i = 0; i < bm25Results.size(); i++) {
            BM25Service.BM25Result result = bm25Results.get(i);
            Document doc = result.document();
            String docId = getDocumentId(doc);
            bm25Ranks.put(docId, i + 1);  // 排名从1开始
            bm25Scores.put(docId, result.score());
        }

        // 2. 计算所有文档的 RRF 分数
        Map<String, HybridSearchResult> scoreMap = new HashMap<>();

        // 处理向量检索结果
        for (Map.Entry<String, Integer> entry : vectorRanks.entrySet()) {
            String docId = entry.getKey();
            int rank = entry.getValue();

            double rrfScore = 1.0 / (RRF_K + rank);

            scoreMap.put(docId, new HybridSearchResult(
                vectorDocs.get(docId),
                rrfScore,
                rank,
                null,  // bm25Rank 稍后填充
                0.0    // bm25Score
            ));
        }

        // 处理 BM25 检索结果，累加 RRF 分数
        for (Map.Entry<String, Integer> entry : bm25Ranks.entrySet()) {
            String docId = entry.getKey();
            int rank = entry.getValue();
            double bm25Score = bm25Scores.get(docId);

            double rrfScore = 1.0 / (RRF_K + rank);

            if (scoreMap.containsKey(docId)) {
                // 文档在两种检索中都存在，累加分数
                HybridSearchResult existing = scoreMap.get(docId);
                scoreMap.put(docId, new HybridSearchResult(
                    existing.getDocument(),
                    existing.getFinalScore() + rrfScore,  // 累加RRF分数
                    existing.getVectorRank(),
                    rank,  // 填充bm25Rank
                    bm25Score
                ));
            } else {
                // 文档只在 BM25 检索中存在
                scoreMap.put(docId, new HybridSearchResult(
                    vectorDocs.getOrDefault(docId, bm25Results.stream()
                        .filter(r -> getDocumentId(r.document()).equals(docId))
                        .findFirst()
                        .map(BM25Service.BM25Result::document)
                        .orElse(null)),
                    rrfScore,
                    null,  // vectorRank为null
                    rank,
                    bm25Score
                ));
            }
        }

        // 3. 按最终分数降序排序，返回 Top-K
        return scoreMap.values().stream()
            .sorted(Comparator.comparingDouble(HybridSearchResult::getFinalScore).reversed())
            .limit(topK)
            .collect(Collectors.toList());
    }

    /**
     * 获取文档ID（用于去重和合并）
     * 优先使用元数据中的ID，否则使用内容哈希
     */
    private String getDocumentId(Document doc) {
        // 尝试从元数据获取ID
        Object id = doc.getMetadata().get("id");
        if (id != null) {
            return id.toString();
        }

        // 使用内容哈希作为ID
        return String.valueOf(doc.getContent().hashCode());
    }

    /**
     * 混合检索结果
     */
    public static class HybridSearchResult {
        private final Document document;
        private final double finalScore;      // RRF融合后的最终分数
        private final Integer vectorRank;     // 在向量检索中的排名（1-based）
        private final Integer bm25Rank;       // 在BM25检索中的排名（1-based）
        private final double bm25Score;       // BM25原始分数

        public HybridSearchResult(
            Document document,
            double finalScore,
            Integer vectorRank,
            Integer bm25Rank,
            double bm25Score
        ) {
            this.document = document;
            this.finalScore = finalScore;
            this.vectorRank = vectorRank;
            this.bm25Rank = bm25Rank;
            this.bm25Score = bm25Score;
        }

        public Document getDocument() {
            return document;
        }

        public double getFinalScore() {
            return finalScore;
        }

        public Integer getVectorRank() {
            return vectorRank;
        }

        public Integer getBm25Rank() {
            return bm25Rank;
        }

        public double getBm25Score() {
            return bm25Score;
        }

        /**
         * 获取向量检索的相似度分数
         */
        public Double getVectorScore() {
            if (document == null) return null;
            return document.getScore();
        }
    }
}
