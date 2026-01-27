package com.example.aidevelop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG 管道配置属性
 * 控制各种 RAG 技巧的启用/禁用和参数
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.chat.rag.pipeline")
public class RagPipelineProperties {

    /**
     * 是否启用查询重写
     * 适用场景：多轮对话、包含代词的查询
     */
    private boolean enableQueryRewrite = true;

    /**
     * 是否启用查询扩展
     * 适用场景：术语不匹配、提升召回率
     */
    private boolean enableQueryExpansion = true;

    /**
     * 是否启用混合检索
     * 适用场景：专有名词、代码、需要精确匹配
     */
    private boolean enableHybridSearch = false;  // 默认关闭，因为需要更多计算

    /**
     * 是否启用重排序
     * 适用场景：问题型查询、需要高准确率
     */
    private boolean enableRerank = false;  // 默认关闭，因为速度慢

    /**
     * 自动模式：根据查询特点自动选择最佳策略
     * true = 智能选择，false = 使用上面配置的固定策略
     */
    private boolean autoMode = true;

    /**
     * 查询长度阈值（超过此长度认为可能是复杂查询）
     */
    private int complexQueryLength = 10;

    /**
     * 问题词列表（包含这些词的查询启用重排序）
     */
    private String[] questionWords = {"怎么办", "如何", "怎么", "为什么", "是什么", "哪些", "如何处理"};
}
