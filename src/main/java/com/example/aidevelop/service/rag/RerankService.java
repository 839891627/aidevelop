package com.example.aidevelop.service.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 重排序服务
 *
 * 核心思想：
 * 1. 先用向量检索召回更多文档（Top-N）
 * 2. 使用 LLM 对召回的文档进行重新打分
 * 3. 返回重新排序后的 Top-K
 *
 * 优势：
 * - 向量检索：快速、理解语义，但可能不够精确
 * - LLM重排：深入理解Query和Document的关系，更准确
 *
 * 实现方案：
 * - 使用 LLM 批量判断文档相关性
 * - 让 LLM 给每个文档打分（0-1）
 * - 按新分数排序，返回 Top-K
 */
@Service
@Slf4j
public class RerankService {

    private final VectorStore vectorStore;
    private final ChatModel chatModel;

    public RerankService(
            VectorStore vectorStore,
            @Qualifier("openAiChatModel") ChatModel chatModel
    ) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
    }

    // 粗筛召回数量（重排序前的数量）
    private static final int RERANK_TOP_N = 20;

    /**
     * 重排序检索主方法
     *
     * @param query 查询文本
     * @param topK 返回前K个结果
     * @return 重排序后的检索结果
     */
    public List<RerankResult> rerankSearch(String query, int topK) {
        log.info("开始重排序检索 - query: '{}', topK: {}", query, topK);

        // 1. 向量检索 - 召回更多文档（粗筛）
        List<Document> candidates = vectorRetrieve(query, RERANK_TOP_N);

        if (candidates.isEmpty()) {
            log.warn("向量检索未返回任何文档");
            return List.of();
        }

        log.info("向量检索召回 {} 个候选文档", candidates.size());

        // 2. LLM 重排序 - 批量打分
        List<RerankResult> rerankedResults = llmRerank(query, candidates);

        // 3. 返回 Top-K
        List<RerankResult> topResults = rerankedResults.stream()
            .limit(topK)
            .collect(Collectors.toList());

        log.info("重排序完成，返回 Top-{} 结果", topResults.size());

        // 打印重排序效果对比
        if (log.isDebugEnabled()) {
            logRerankComparison(candidates, topResults);
        }

        return topResults;
    }

    /**
     * 向量检索（粗筛）
     */
    private List<Document> vectorRetrieve(String query, int topN) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topN)
                .similarityThreshold(0.0)  // 降低阈值，召回更多
                .build();

            return vectorStore.similaritySearch(searchRequest);
        } catch (Exception e) {
            log.error("向量检索失败", e);
            return List.of();
        }
    }

    /**
     * LLM 重排序
     *
     * 核心思路：
     * - 将所有候选文档发送给 LLM
     * - 让 LLM 判断每个文档与查询的相关性（0-1分）
     * - 按 LLM 打分重新排序
     */
    private List<RerankResult> llmRerank(String query, List<Document> candidates) {
        log.debug("开始 LLM 重排序，共 {} 个候选文档", candidates.size());

        // 1. 构建提示词
        String prompt = buildRerankPrompt(query, candidates);

        try {
            // 2. 调用 LLM
            String response = chatModel.call(prompt);

            // 3. 解析 LLM 返回的打分结果
            return parseLlmResponse(response, candidates);

        } catch (Exception e) {
            log.error("LLM 重排序失败，返回向量检索原始结果", e);
            // 降级：返回向量检索的原始结果
            return candidates.stream()
                .map(doc -> new RerankResult(
                    doc,
                    doc.getScore() != null ? doc.getScore() : 0.0,
                    false  // 未经过LLM重排
                ))
                .collect(Collectors.toList());
        }
    }

    /**
     * 构建重排序提示词
     */
    private String buildRerankPrompt(String query, List<Document> candidates) {
        StringBuilder sb = new StringBuilder();

        sb.append("你是一个信息检索专家。请根据用户查询，对以下文档片段进行相关性评分。\n\n");
        sb.append("## 用户查询\n");
        sb.append(query).append("\n\n");

        sb.append("## 文档片段\n");
        for (int i = 0; i < candidates.size(); i++) {
            Document doc = candidates.get(i);
            sb.append(String.format("### [%d] ", i + 1));
            sb.append(doc.getText().substring(0, Math.min(200, doc.getText().length())));
            if (doc.getText().length() > 200) {
                sb.append("...");
            }
            sb.append("\n\n");
        }

        sb.append("## 评分规则\n");
        sb.append("- 评分范围：0.0 - 1.0\n");
        sb.append("- 1.0：完全相关，直接回答了查询问题\n");
        sb.append("- 0.7-0.9：高度相关，包含有用信息\n");
        sb.append("- 0.4-0.6：部分相关，包含一些有用信息\n");
        sb.append("- 0.1-0.3：弱相关，只有少量有用信息\n");
        sb.append("- 0.0：不相关\n\n");

        sb.append("## 输出格式\n");
        sb.append("请按照以下格式输出（每行一个）：\n");
        sb.append("[文档编号] [分数]\n\n");
        sb.append("示例：\n");
        sb.append("[1] 0.85\n");
        sb.append("[2] 0.32\n");
        sb.append("[3] 0.91\n\n");

        sb.append("现在开始评分：\n");

        return sb.toString();
    }

    /**
     * 解析 LLM 返回的打分结果
     */
    private List<RerankResult> parseLlmResponse(String response, List<Document> candidates) {
        List<RerankResult> results = new java.util.ArrayList<>();

        try {
            // 解析格式：[编号] 分数
            String[] lines = response.split("\n");

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // 匹配格式：[1] 0.85 或 1) 0.85 或 1. 0.85
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[?(\\d+)\\]?[.)\\s]+([0-9.]+)");
                java.util.regex.Matcher matcher = pattern.matcher(line);

                if (matcher.find()) {
                    int index = Integer.parseInt(matcher.group(1)) - 1;  // 转为0-based

                    if (index >= 0 && index < candidates.size()) {
                        double score = Double.parseDouble(matcher.group(2));
                        Document doc = candidates.get(index);

                        results.add(new RerankResult(
                            doc,
                            score,
                            true  // 经过LLM重排
                        ));

                        log.debug("文档 [{}] 重排序分数: {}", index + 1, score);
                    }
                }
            }

            // 如果解析失败，返回原始结果
            if (results.isEmpty()) {
                log.warn("LLM 返回格式无法解析，返回向量检索原始结果");
                return candidates.stream()
                    .map(doc -> new RerankResult(
                        doc,
                        doc.getScore() != null ? doc.getScore() : 0.0,
                        false
                    ))
                    .collect(Collectors.toList());
            }

            // 按分数降序排序
            results.sort((a, b) -> Double.compare(b.rerankScore(), a.rerankScore()));

            return results;

        } catch (Exception e) {
            log.error("解析 LLM 响应失败", e);
            // 降级：返回向量检索的原始结果
            return candidates.stream()
                .map(doc -> new RerankResult(
                    doc,
                    doc.getScore() != null ? doc.getScore() : 0.0,
                    false
                ))
                .collect(Collectors.toList());
        }
    }

    /**
     * 打印重排序前后对比（调试用）
     */
    private void logRerankComparison(List<Document> original, List<RerankResult> reranked) {
        log.debug("=== 重排序效果对比 ===");

        log.debug("向量检索 Top-3：");
        original.stream().limit(3).forEach(doc ->
            log.debug("  - score={}, content={}",
                doc.getScore(),
                doc.getText().substring(0, Math.min(50, doc.getText().length()))
            )
        );

        log.debug("LLM重排后 Top-3：");
        reranked.stream().limit(3).forEach(result ->
            log.debug("  - rerank_score={}, vector_score={}, reranked={}, content={}",
                result.rerankScore(),
                result.getVectorScore(),
                result.reranked(),
                result.document().getText().substring(0, Math.min(50, result.document().getText().length()))
            )
        );
    }

    /**
     * 重排序结果
     */
    public record RerankResult(
        Document document,
        double rerankScore,    // LLM重排序后的分数
        boolean reranked       // 是否经过LLM重排（false表示降级为向量检索）
    ) {
        /**
         * 获取向量检索的原始分数
         */
        public double getVectorScore() {
            return document.getScore() != null ? document.getScore() : 0.0;
        }

        /**
         * 分数提升幅度
         */
        public double getScoreImprovement() {
            return rerankScore - getVectorScore();
        }
    }
}
