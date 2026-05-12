package com.example.aidevelop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 对话意图路由配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.chat.routing")
public class RouteProperties {

    /**
     * 识别业务订单/用户编号的正则。
     */
    private String businessIdPattern = "(LOAN\\d+|REPAY\\d+|CUST\\d+|ORDER\\d+)";

    /**
     * 命中后优先走工具的关键词。
     */
    private List<String> toolIntentKeywords = List.of(
        "查询订单", "订单", "借款记录", "还款记录", "风险评估", "用户编号"
    );

    /**
     * 命中后优先走知识检索的关键词。
     */
    private List<String> ragIntentKeywords = List.of(
        "规则", "政策", "产品手册", "利率", "额度", "期限", "申请条件", "流程"
    );

    /**
     * TOOL_ONLY 路由允许的工具列表。
     */
    private List<String> toolOnlyToolNames = List.of(
        "loanQueryFunction", "repaymentQueryFunction", "riskAssessmentFunction"
    );

    /**
     * HYBRID 路由允许的工具列表。
     */
    private List<String> hybridToolNames = List.of(
        "loanQueryFunction", "repaymentQueryFunction", "riskAssessmentFunction"
    );

    /**
     * RAG_ONLY 的检索参数。
     */
    private int ragOnlyTopK = 5;
    private double ragOnlySimilarityThreshold = 0.2;

    /**
     * HYBRID 的检索参数（通常比纯 RAG 更保守一些）。
     */
    private int hybridTopK = 3;
    private double hybridSimilarityThreshold = 0.25;

    /**
     * 路由层运行参数（供日志/治理使用）。
     */
    private int maxToolCalls = 3;
    private int timeoutMs = 30000;
}
