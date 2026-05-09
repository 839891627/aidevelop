package com.example.aidevelop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG（检索增强生成）相关配置属性
 * 对应 application.yml 中的 app.chat.rag 配置段
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.chat.rag")
public class RagProperties {

    /**
     * 是否启用 RAG 检索增强
     */
    private boolean enabled = true;

    /**
     * 相似度阈值，用于过滤检索结果
     * 范围：0.0 - 1.0
     * 说明：ZhipuAI 对于"大段文本+短查询"的场景，分数通常较低 (0.2-0.3)，
     *      建议设置为 0.2 以确保能检索到相关文档。
     */
    private double similarityThreshold = 0.2;

    /**
     * 检索返回的 Top K 文档数量
     */
    private int topK = 5;
}