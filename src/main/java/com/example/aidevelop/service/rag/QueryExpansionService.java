package com.example.aidevelop.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 查询扩展服务
 *
 * 功能：通过同义词扩展、专业术语替换等方式，提升 RAG 检索的召回率
 *
 * 适用场景：
 * - 用户使用口语化表达（如"黑名单"）
 * - 文档中使用专业术语（如"征信上报"、"不良记录"）
 * - 两者不匹配导致检索失败
 */
@Service
@Slf4j
public class QueryExpansionService {

    /**
     * 同义词词典
     * key: 常见用户查询词
     * value: 对应的专业术语（多个）
     */
    private static final Map<String, List<String>> SYNONYMS = new HashMap<>();

    /**
     * 专业术语词典
     * key: 专业术语
     * value: 用户可能使用的表达
     */
    private static final Map<String, List<String>> TECHNICAL_TERMS = new HashMap<>();

    static {
        // === 核心业务术语 ===
        SYNONYMS.put("黑名单", List.of(
            "征信黑名单",
            "不良记录",
            "征信不良",
            "失信名单",
            "失信被执行人"
        ));

        SYNONYMS.put("逾期", List.of(
            "OVERDUE",
            "拖欠",
            "欠款",
            "违约",
            "未按时还款",
            "延期还款"
        ));

        SYNONYMS.put("利率", List.of(
            "利息",
            "费率",
            "年化",
            "年化利率",
            "APR"
        ));

        SYNONYMS.put("提前还款", List.of(
            "提前结清",
            "早期还款",
            "部分提前还款",
            "提前结清贷款"
        ));

        SYNONYMS.put("借款", List.of(
            "贷款",
            "信贷",
            "放款",
            "贷款发放"
        ));

        SYNONYMS.put("还款", List.of(
            "偿还",
            "归还",
            "清偿",
            "结算"
        ));

        SYNONYMS.put("催收", List.of(
            "追缴",
            "追讨",
            "催缴",
            "收账"
        ));

        SYNONYMS.put("征信", List.of(
            "信用报告",
            "信用记录",
            "人行征信",
            "个人征信"
        ));

        SYNONYMS.put("合同", List.of(
            "协议",
            "借款合同",
            "信贷协议"
        ));

        SYNONYMS.put("客户", List.of(
            "借款人",
            "申请人",
            "用户"
        ));

        // === 反向词典：专业术语 → 用户表达 ===
        TECHNICAL_TERMS.put("M1", List.of("第一阶段", "初期阶段", "1-30天"));
        TECHNICAL_TERMS.put("M2", List.of("第二阶段", "中期阶段", "31-90天"));
        TECHNICAL_TERMS.put("M3", List.of("第三阶段", "后期阶段", "91天以上", "严重逾期"));

        TECHNICAL_TERMS.put("征信上报", List.of("黑名单", "不良记录", "征信不良"));
        TECHNICAL_TERMS.put("不良资产", List.of("坏账", "呆账", "损失类贷款"));
    }

    /**
     * 查询扩展主方法
     *
     * @param originalQuery 原始用户查询
     * @return 扩展后的查询（使用 OR 连接）
     */
    public String expandQuery(String originalQuery) {
        if (originalQuery == null || originalQuery.trim().isEmpty()) {
            return originalQuery;
        }

        Set<String> expandedTerms = new LinkedHashSet<>();
        expandedTerms.add(originalQuery);

        // 1. 基于同义词的扩展
        for (Map.Entry<String, List<String>> entry : SYNONYMS.entrySet()) {
            if (originalQuery.contains(entry.getKey())) {
                expandedTerms.addAll(entry.getValue());
                log.debug("同义词扩展: {} -> 添加 {}", entry.getKey(), entry.getValue());
            }
        }

        // 2. 基于专业术语的反向扩展
        for (Map.Entry<String, List<String>> entry : TECHNICAL_TERMS.entrySet()) {
            if (originalQuery.contains(entry.getKey())) {
                expandedTerms.addAll(entry.getValue());
                log.debug("专业术语扩展: {} -> 添加 {}", entry.getKey(), entry.getValue());
            }
        }

        // 3. 构建 OR 查询
        String expandedQuery = String.join(" OR ", expandedTerms);

        log.info("查询扩展: {} -> {}", originalQuery, expandedQuery);
        return expandedQuery;
    }

    /**
     * 获取查询扩展的详细信息（用于调试）
     *
     * @param originalQuery 原始查询
     * @return 扩展详情
     */
    public ExpansionDetail getExpansionDetail(String originalQuery) {
        Set<String> expandedTerms = new LinkedHashSet<>();
        List<String> matchedSynonyms = new ArrayList<>();
        List<String> matchedTechnical = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : SYNONYMS.entrySet()) {
            if (originalQuery.contains(entry.getKey())) {
                matchedSynonyms.add(entry.getKey() + " → " + entry.getValue());
                expandedTerms.addAll(entry.getValue());
            }
        }

        for (Map.Entry<String, List<String>> entry : TECHNICAL_TERMS.entrySet()) {
            if (originalQuery.contains(entry.getKey())) {
                matchedTechnical.add(entry.getKey() + " → " + entry.getValue());
                expandedTerms.addAll(entry.getValue());
            }
        }

        return new ExpansionDetail(
            originalQuery,
            String.join(" OR ", expandedTerms),
            matchedSynonyms,
            matchedTechnical
        );
    }

    /**
     * 查询扩展详情
     */
    public record ExpansionDetail(
            String originalQuery,
            String expandedQuery,
            List<String> synonymExpansions,
            List<String> technicalExpansions
    ) {
        public String getOriginalQuery() { return originalQuery; }
        public String getExpandedQuery() { return expandedQuery; }
        public List<String> getSynonymExpansions() { return synonymExpansions; }
        public List<String> getTechnicalExpansions() { return technicalExpansions; }
    }

    /**
     * 检查查询是否包含特定业务术语
     *
     * @param query 查询字符串
     * @param businessTerm 业务术语
     * @return 是否包含
     */
    public boolean containsBusinessTerm(String query, String businessTerm) {
        return query != null && query.contains(businessTerm);
    }

    /**
     * 批量查询扩展
     *
     * @param queries 查询列表
     * @return 扩展后的查询映射（原始查询 → 扩展查询）
     */
    public Map<String, String> batchExpand(List<String> queries) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String query : queries) {
            result.put(query, expandQuery(query));
        }
        return result;
    }

    /**
     * 获取所有支持的同义词映射
     *
     * @return 同义词词典（只读副本）
     */
    public Map<String, List<String>> getSynonymDictionary() {
        return Map.copyOf(SYNONYMS);
    }

    /**
     * 获取所有专业术语映射
     *
     * @return 专业术语词典（只读副本）
     */
    public Map<String, List<String>> getTechnicalTermDictionary() {
        return Map.copyOf(TECHNICAL_TERMS);
    }
}
