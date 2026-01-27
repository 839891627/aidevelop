package com.example.aidevelop.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 查询评估实体
 * 用于评估 RAG 系统的检索效果
 */
@Data
@Entity
@Table(name = "query_evaluation")
public class QueryEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 查询文本
     */
    @Column(name = "query", nullable = false, length = 500)
    private String query;

    /**
     * 相关文档ID列表（用逗号分隔）
     * 这些是应该被检索到的文档
     */
    @Column(name = "relevant_doc_ids", columnDefinition = "TEXT")
    private String relevantDocIds;

    /**
     * 查询类别
     * 例如：简单查询、复杂查询、问题型查询、专有名词查询
     */
    @Column(name = "category", length = 50)
    private String category;

    /**
     * 难度等级（1-5，5最难）
     */
    @Column(name = "difficulty")
    private Integer difficulty;

    /**
     * 期望的最低召回率
     */
    @Column(name = "expected_recall")
    private Double expectedRecall;

    /**
     * 期望的最低精确率
     */
    @Column(name = "expected_precision")
    private Double expectedPrecision;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 评估结果
     */
    @Column(name = "evaluation_result", columnDefinition = "TEXT")
    private String evaluationResult;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
