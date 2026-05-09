package com.example.aidevelop.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RAG 系统评估服务
 * <p>
 * 功能：评估 RAG 系统的检索效果，计算各种评估指标
 * <p>
 * 评估指标：
 * - 召回率（Recall）：检索到的相关文档占所有相关文档的比例
 * - 精确率（Precision）：检索到的文档中真正相关的比例
 * - F1 分数：召回率和精确率的调和平均
 * - MRR（Mean Reciprocal Rank）：第一个相关文档的平均排名倒数
 * - NDCG（Normalized Discounted Cumulative Gain）：考虑排序位置和相关性等级
 */
@Service
@Slf4j
public class RagEvaluationService {

    private final VectorStore vectorStore;

    @Value("${app.chat.rag.similarity-threshold:0.2}")
    private double similarityThreshold;

    public RagEvaluationService(@Qualifier("vectorStore") VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 评估单个查询的检索效果
     *
     * @param query          查询文本
     * @param relevantDocIds 相关文档ID列表（应该被检索到的文档）
     * @param topK           检索返回的文档数量
     * @return 评估结果
     */
    public EvaluationMetrics evaluate(String query, List<String> relevantDocIds, int topK) {
        log.info("开始评估查询: {}", query);
        log.info("相关文档数: {}, Top-K: {}", relevantDocIds.size(), topK);

        // 1. 执行检索
        List<Document> retrievedDocs = retrieveDocuments(query, topK);

        // 2. 获取检索到的文档ID
        Set<String> retrievedDocIds = retrievedDocs.stream()
                .map(this::getDocumentId)
                .collect(Collectors.toSet());

        // 3. 计算各种指标
        double recall = calculateRecall(retrievedDocIds, relevantDocIds);
        double precision = calculatePrecision(retrievedDocIds, relevantDocIds);
        double f1 = calculateF1(recall, precision);
        double mrr = calculateMRR(retrievedDocs, relevantDocIds);
        double ndcg = calculateNDCG(retrievedDocs, relevantDocIds, topK);

        // 4. 构建评估结果
        EvaluationMetrics metrics = new EvaluationMetrics(
                query,
                topK,
                recall,
                precision,
                f1,
                mrr,
                ndcg,
                retrievedDocs.size(),
                relevantDocIds.size(),
                buildDetailedInfo(retrievedDocs, relevantDocIds, retrievedDocIds)
        );

        log.info("评估完成 - 召回率: {:.2f}, 精确率: {:.2f}, F1: {:.2f}, MRR: {:.2f}, NDCG: {:.2f}",
                recall, precision, f1, mrr, ndcg);

        return metrics;
    }

    /**
     * 执行检索
     */
    private List<Document> retrieveDocuments(String query, int topK) {
        SearchRequest searchRequest = SearchRequest.query(query)
                .withTopK(topK)
                .withSimilarityThreshold(similarityThreshold);

        return vectorStore.similaritySearch(searchRequest);
    }

    /**
     * 计算召回率
     * Recall = 检索到的相关文档数 / 所有相关文档数
     */
    private double calculateRecall(Set<String> retrievedDocIds, List<String> relevantDocIds) {
        if (relevantDocIds.isEmpty()) {
            return 1.0;  // 没有相关文档，认为全部召回
        }

        long retrievedRelevant = relevantDocIds.stream()
                .filter(retrievedDocIds::contains)
                .count();

        return (double) retrievedRelevant / relevantDocIds.size();
    }

    /**
     * 计算精确率
     * Precision = 检索到的相关文档数 / 检索到的总文档数
     */
    private double calculatePrecision(Set<String> retrievedDocIds, List<String> relevantDocIds) {
        if (retrievedDocIds.isEmpty()) {
            return 0.0;  // 没有检索到任何文档
        }

        long retrievedRelevant = relevantDocIds.stream()
                .filter(retrievedDocIds::contains)
                .count();

        return (double) retrievedRelevant / retrievedDocIds.size();
    }

    /**
     * 计算 F1 分数
     * F1 = 2 * (Precision * Recall) / (Precision + Recall)
     */
    private double calculateF1(double recall, double precision) {
        if (recall + precision == 0) {
            return 0.0;
        }
        return 2 * (precision * recall) / (precision + recall);
    }

    /**
     * 计算 MRR (Mean Reciprocal Rank)
     * MRR = 1 / 第一个相关文档的排名
     */
    private double calculateMRR(List<Document> retrievedDocs, List<String> relevantDocIds) {
        if (relevantDocIds.isEmpty()) {
            return 0.0;
        }

        for (int i = 0; i < retrievedDocs.size(); i++) {
            String docId = getDocumentId(retrievedDocs.get(i));
            if (relevantDocIds.contains(docId)) {
                // 找到第一个相关文档
                return 1.0 / (i + 1);  // 排名从1开始
            }
        }

        return 0.0;  // 没有检索到任何相关文档
    }

    /**
     * 计算 NDCG (Normalized Discounted Cumulative Gain)
     * 简化版本：假设所有相关文档的相关度等级为1
     */
    private double calculateNDCG(List<Document> retrievedDocs, List<String> relevantDocIds, int topK) {
        if (relevantDocIds.isEmpty()) {
            return 1.0;  // 没有相关文档，NDCG为1
        }

        // 计算 DCG (Discounted Cumulative Gain)
        double dcg = 0.0;
        for (int i = 0; i < Math.min(topK, retrievedDocs.size()); i++) {
            String docId = getDocumentId(retrievedDocs.get(i));
            boolean isRelevant = relevantDocIds.contains(docId);

            // DCG = rel1 / log2(1) + rel2 / log2(2) + rel3 / log2(3) + ...
            // 相关度等级：相关=1，不相关=0
            double relevance = isRelevant ? 1.0 : 0.0;
            dcg += relevance / (Math.log(i + 2) / Math.log(2));
        }

        // 计算 IDCG (Ideal DCG) - 假设所有相关文档都排在最前面
        double idcg = 0.0;
        int relevantCount = Math.min(topK, relevantDocIds.size());
        for (int i = 0; i < relevantCount; i++) {
            idcg += 1.0 / (Math.log(i + 2) / Math.log(2));
        }

        // NDCG = DCG / IDCG
        return idcg == 0 ? 0.0 : dcg / idcg;
    }

    /**
     * 构建详细信息
     */
    private String buildDetailedInfo(List<Document> retrievedDocs, List<String> relevantDocIds,
                                     Set<String> retrievedDocIds) {
        StringBuilder sb = new StringBuilder();

        sb.append("检索到的文档:\n");
        for (int i = 0; i < Math.min(5, retrievedDocs.size()); i++) {
            Document doc = retrievedDocs.get(i);
            String docId = getDocumentId(doc);
            boolean isRelevant = relevantDocIds.contains(docId);

            sb.append(String.format("  [%d] ID=%s, 相关=%s, 内容=%s\n",
                    i + 1,
                    docId,
                    isRelevant ? "✓" : "✗",
                    doc.getContent().substring(0, Math.min(50, doc.getContent().length()))
            ));
        }

        if (retrievedDocs.size() > 5) {
            sb.append(String.format("  ... 还有 %d 个文档\n", retrievedDocs.size() - 5));
        }

        return sb.toString();
    }

    /**
     * 获取文档ID
     * 优先使用元数据中的ID，否则使用内容哈希
     */
    private String getDocumentId(Document doc) {
        Object id = doc.getMetadata().get("id");
        if (id != null) {
            return id.toString();
        }

        // 使用内容的前50个字符的哈希作为ID
        String content = doc.getContent();
        String contentPrefix = content.substring(0, Math.min(50, content.length()));
        return String.valueOf(contentPrefix.hashCode());
    }

    /**
     * 批量评估多个查询
     *
     * @param queries 查询列表及其相关文档
     * @param topK    检索返回的文档数量
     * @return 批量评估结果
     */
    public BatchEvaluationResult batchEvaluate(List<QueryWithRelevantDocs> queries, int topK) {
        log.info("开始批量评估，共 {} 个查询", queries.size());

        List<EvaluationMetrics> allMetrics = new ArrayList<>();

        for (QueryWithRelevantDocs query : queries) {
            try {
                EvaluationMetrics metrics = evaluate(query.query(), query.relevantDocIds(), topK);
                allMetrics.add(metrics);
            } catch (Exception e) {
                log.error("评估查询失败: {}", query.query(), e);
            }
        }

        // 计算平均指标
        double avgRecall = allMetrics.stream().mapToDouble(EvaluationMetrics::recall).average().orElse(0);
        double avgPrecision = allMetrics.stream().mapToDouble(EvaluationMetrics::precision).average().orElse(0);
        double avgF1 = allMetrics.stream().mapToDouble(EvaluationMetrics::f1).average().orElse(0);
        double avgMRR = allMetrics.stream().mapToDouble(EvaluationMetrics::mrr).average().orElse(0);
        double avgNDCG = allMetrics.stream().mapToDouble(EvaluationMetrics::ndcg).average().orElse(0);

        BatchEvaluationResult result = new BatchEvaluationResult(
                queries.size(),
                topK,
                avgRecall,
                avgPrecision,
                avgF1,
                avgMRR,
                avgNDCG,
                allMetrics
        );

        log.info("批量评估完成 - 平均召回率: {:.2f}, 平均精确率: {:.2f}, 平均F1: {:.2f}",
                avgRecall, avgPrecision, avgF1);

        return result;
    }

    /**
     * 查询及其相关文档
     */
    public record QueryWithRelevantDocs(
            String query,
            List<String> relevantDocIds
    ) {
    }

    /**
     * 评估指标
     */
    public record EvaluationMetrics(
            String query,
            int topK,
            double recall,           // 召回率
            double precision,        // 精确率
            double f1,               // F1分数
            double mrr,              // 平均倒数排名
            double ndcg,             // 归一化折损累计增益
            int retrievedCount,      // 检索到的文档数
            int relevantCount,       // 相关文档总数
            String detailedInfo      // 详细信息
    ) {
        /**
         * 是否达到目标指标
         */
        public boolean meetsTarget(double minRecall, double minPrecision) {
            return recall >= minRecall && precision >= minPrecision;
        }
    }

    /**
     * 批量评估结果
     */
    public record BatchEvaluationResult(
            int totalQueries,
            int topK,
            double avgRecall,
            double avgPrecision,
            double avgF1,
            double avgMRR,
            double avgNDCG,
            List<EvaluationMetrics> allMetrics
    ) {
    }
}
