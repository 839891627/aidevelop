package com.example.aidevelop.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * BM25 关键词检索服务
 *
 * BM25 是一种经典的搜索引擎算法，用于关键词匹配
 * 相比向量检索，BM25 更擅长：
 * 1. 精确匹配专有名词（如 "M1"、"CUST001"）
 * 2. 匹配数字、代码等非语义内容
 * 3. 处理短查询（如产品编号）
 *
 * BM25 公式：
 * score(D,Q) = Σ IDF(qi) × (f(qi,D) × (k1 + 1)) / (f(qi,D) + k1 × (1 - b + b × |D| / avgdl))
 *
 * 其中：
 * - qi: 查询中的第i个词
 * - f(qi,D): 词qi在文档D中的频率
 * - |D|: 文档D的长度（词数）
 * - avgdl: 所有文档的平均长度
 * - k1: 调节词频饱和度的参数（通常1.2-2.0）
 * - b: 调节文档长度归一化的参数（通常0.75）
 * - IDF(qi): 逆文档频率 = log((N - df(qi) + 0.5) / (df(qi) + 0.5))
 */
@Service
@Slf4j
public class BM25Service {

    private final VectorStore vectorStore;
    private volatile boolean indexInitialized = false;

    // BM25 参数
    private static final double K1 = 1.2;  // 词频饱和度参数
    private static final double B = 0.75;  // 长度归一化参数

    // 文档统计信息
    private List<DocumentInfo> documentInfos;
    private double avgDocumentLength;
    private Map<String, Integer> documentFrequency;  // 词 → 包含该词的文档数

    public BM25Service(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.documentInfos = new ArrayList<>();
        this.avgDocumentLength = 0;
        this.documentFrequency = new HashMap<>();
        // 改为懒加载，避免应用启动时触发远程 Embedding 调用
        // （当混合检索关闭时，BM25 不会实际使用）
    }

    /**
     * 初始化 BM25 索引
     * 从向量库加载所有文档，构建词频统计
     */
    private void initializeIndex() {
        log.info("开始初始化 BM25 索引...");

        try {
            // 1. 从向量库获取所有文档
            // 使用通配符查询来获取所有文档
            var allDocs = vectorStore.similaritySearch(
                org.springframework.ai.vectorstore.SearchRequest.builder()
                    .query(".")
                    .topK(1000)  // 假设不超过1000个文档片段
                    .similarityThreshold(0.0)
                    .build()
            );

            if (allDocs.isEmpty()) {
                log.warn("向量库中没有文档，BM25 索引为空");
                this.documentInfos = new ArrayList<>();
                this.avgDocumentLength = 0;
                this.documentFrequency = new HashMap<>();
                return;
            }

            // 2. 构建文档信息（分词、词频统计）
            this.documentInfos = allDocs.stream()
                .map(DocumentInfo::new)
                .collect(Collectors.toList());

            // 3. 计算平均文档长度
            this.avgDocumentLength = documentInfos.stream()
                .mapToInt(di -> di.termFrequency.size())
                .average()
                .orElse(0.0);

            // 4. 计算文档频率（DF）：每个词出现在多少个文档中
            this.documentFrequency = calculateDocumentFrequency();

            log.info("BM25 索引初始化完成：共 {} 个文档片段，平均长度 {} 词",
                documentInfos.size(), (int) avgDocumentLength);

        } catch (Exception e) {
            log.warn("BM25 索引初始化失败，将降级为空索引：{}", e.getMessage());
            this.documentInfos = new ArrayList<>();
            this.avgDocumentLength = 0;
            this.documentFrequency = new HashMap<>();
        } finally {
            this.indexInitialized = true;
        }
    }

    /**
     * 懒加载初始化，避免启动时触发外部调用
     */
    private void ensureIndexInitialized() {
        if (indexInitialized) {
            return;
        }

        synchronized (this) {
            if (!indexInitialized) {
                initializeIndex();
            }
        }
    }

    /**
     * 计算文档频率（DF）
     * DF(term) = 包含该词的文档数量
     */
    private Map<String, Integer> calculateDocumentFrequency() {
        Map<String, Integer> df = new HashMap<>();

        for (DocumentInfo docInfo : documentInfos) {
            for (String term : docInfo.termFrequency.keySet()) {
                df.put(term, df.getOrDefault(term, 0) + 1);
            }
        }

        return df;
    }

    /**
     * BM25 检索
     *
     * @param query 查询文本
     * @param topK 返回前K个结果
     * @return 文档列表及其 BM25 分数
     */
    public List<BM25Result> search(String query, int topK) {
        ensureIndexInitialized();

        if (documentInfos.isEmpty()) {
            log.warn("BM25 索引为空，返回空结果");
            return new ArrayList<>();
        }

        // 1. 分词（简单的空格和标点符号分词）
        List<String> queryTerms = tokenize(query);

        if (queryTerms.isEmpty()) {
            return new ArrayList<>();
        }

        log.debug("BM25 查询分词: {}", queryTerms);

        // 2. 计算每个文档的 BM25 分数
        List<BM25Result> results = new ArrayList<>();

        for (DocumentInfo docInfo : documentInfos) {
            double score = calculateBM25Score(queryTerms, docInfo);

            if (score > 0) {
                results.add(new BM25Result(
                    docInfo.document,
                    score
                ));
            }
        }

        // 3. 按分数降序排序，返回 Top-K
        return results.stream()
            .sorted(Comparator.comparingDouble(BM25Result::score).reversed())
            .limit(topK)
            .collect(Collectors.toList());
    }

    /**
     * 计算 BM25 分数
     */
    private double calculateBM25Score(List<String> queryTerms, DocumentInfo docInfo) {
        double score = 0.0;
        int docLength = docInfo.termFrequency.size();

        for (String term : queryTerms) {
            // 1. 词频（TF）：词在文档中的出现次数
            int termFreq = docInfo.termFrequency.getOrDefault(term, 0);

            if (termFreq == 0) {
                continue;  // 文档中没有这个词，跳过
            }

            // 2. 文档频率（DF）：包含该词的文档数
            int df = documentFrequency.getOrDefault(term, 0);

            // 3. 逆文档频率（IDF）
            double idf = Math.log(
                (documentInfos.size() - df + 0.5) / (df + 0.5) + 1.0
            );

            // 4. BM25 公式
            double tfComponent = termFreq * (K1 + 1) / (
                termFreq + K1 * (1 - B + B * docLength / avgDocumentLength)
            );

            score += idf * tfComponent;
        }

        return score;
    }

    /**
     * 分词（简单实现）
     * 中文和英文的分词策略不同：
     * - 英文：按空格和标点符号分词
     * - 中文：按字符分词（简化实现）
     *
     * 生产环境建议使用专业的分词器：
     * - 中文：jieba、HanLP
     * - 英文：Lucene StandardTokenizer
     */
    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();

        // 简单分词：按空格、标点符号分割
        String[] parts = text.split("[\\s\\p{Punct}]+");

        for (String part : parts) {
            if (part.isEmpty()) continue;

            // 判断是中文还是英文
            if (isChinese(part)) {
                // 中文：按字符分词（2-3字的词组）
                if (part.length() >= 2) {
                    // 添加2字词组
                    for (int i = 0; i < part.length() - 1; i++) {
                        tokens.add(part.substring(i, i + 2));
                    }
                    // 添加3字词组
                    if (part.length() >= 3) {
                        for (int i = 0; i < part.length() - 2; i++) {
                            tokens.add(part.substring(i, i + 3));
                        }
                    }
                }
                // 添加单个字符
                for (char c : part.toCharArray()) {
                    if (c > 0x4E00 && c < 0x9FA5) {  // 中文字符范围
                        tokens.add(String.valueOf(c));
                    }
                }
            } else {
                // 英文/数字：直接添加，并转为小写
                tokens.add(part.toLowerCase());
            }
        }

        return tokens;
    }

    /**
     * 判断字符串是否包含中文
     */
    private boolean isChinese(String text) {
        return text.matches(".*[\\u4e00-\\u9fa5].*");
    }

    /**
     * 文档信息（用于 BM25 计算）
     */
    private static class DocumentInfo {
        final Document document;
        final Map<String, Integer> termFrequency;  // 词频统计

        DocumentInfo(Document document) {
            this.document = document;
            this.termFrequency = calculateTermFrequency(document.getText());
        }

        /**
         * 计算词频（TF）
         * TF(term) = 词在文档中出现的次数
         */
        private Map<String, Integer> calculateTermFrequency(String content) {
            Map<String, Integer> tf = new HashMap<>();

            // 简单分词（与 tokenize 方法保持一致）
            String[] parts = content.split("[\\s\\p{Punct}]+");

            for (String part : parts) {
                if (part.isEmpty()) continue;

                if (isChinese(part)) {
                    // 中文：按字符分词
                    for (int i = 0; i < part.length() - 1; i++) {
                        String bigram = part.substring(i, i + 2);
                        tf.put(bigram, tf.getOrDefault(bigram, 0) + 1);
                    }
                    for (char c : part.toCharArray()) {
                        if (c > 0x4E00 && c < 0x9FA5) {
                            tf.put(String.valueOf(c), tf.getOrDefault(String.valueOf(c), 0) + 1);
                        }
                    }
                } else {
                    // 英文/数字
                    tf.put(part.toLowerCase(), tf.getOrDefault(part.toLowerCase(), 0) + 1);
                }
            }

            return tf;
        }

        private boolean isChinese(String text) {
            return text.matches(".*[\\u4e00-\\u9fa5].*");
        }
    }

    /**
     * BM25 检索结果
     */
    public record BM25Result(
        Document document,
        double score
    ) {}
}
