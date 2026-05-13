package com.example.aidevelop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai.agent")
public class AgentProperties {

    /**
     * Agent 入口总开关。
     */
    private boolean enabled = true;

    /**
     * 单次请求最大执行步数。
     */
    private int maxSteps = 3;

    /**
     * 工具调用超时（毫秒）。
     */
    private int timeoutMs = 15000;

    /**
     * 工具失败重试次数（不含首次调用）。
     */
    private int toolMaxRetries = 1;

    /**
     * 重试退避毫秒数。
     */
    private int retryBackoffMs = 200;

    /**
     * 是否启用 Reflect 阶段。
     */
    private boolean reflectEnabled = true;

    /**
     * 是否启用二次改计划（replan）。
     */
    private boolean replanEnabled = true;

    /**
     * 最多二次改计划轮次。
     */
    private int maxReplanRounds = 1;

    /**
     * 风险评估类问题是否强制包含 RAG 检索证据。
     */
    private boolean forceRagForRiskEvaluation = true;

    /**
     * 风险意图关键词（命中后触发策略规则）。
     */
    private List<String> riskIntentKeywords = new ArrayList<>(List.of(
        "风险",
        "风控",
        "评估",
        "判断"
    ));

    /**
     * 风险问题自检是否要求包含 RAG 证据。
     */
    private boolean requireRagEvidenceForRisk = true;

    /**
     * 是否启用结果自检。
     */
    private boolean selfCheckEnabled = true;

    /**
     * 自检通过最低分（0-100）。
     */
    private int selfCheckMinScore = 70;

    /**
     * 判定可直接高置信回答所需的最少观察条数。
     */
    private int minObservationCount = 1;

    /**
     * 二次改计划失败时是否强制回退到高置信模板回答。
     */
    private boolean fallbackOnReplanFailure = true;

    /**
     * 自检未通过时是否回退到高置信模板回答。
     */
    private boolean fallbackWhenSelfCheckFailed = true;

    /**
     * 高置信回退回答模板，支持占位符：
     * {question} {routeType} {observations} {reason}
     */
    private String fallbackAnswerTemplate = """
        基于当前可验证信息，我先给出稳健结论：
        - 问题：{question}
        - 路由：{routeType}
        - 证据：{observations}
        - 说明：{reason}
        如需更精确结果，请补充更多上下文或稍后重试。
        """;

    /**
     * 允许 Agent 调用的工具列表。
     */
    private List<String> allowedTools = new ArrayList<>(List.of(
        "rag.search",
        "loan.query",
        "repayment.query",
        "risk.assess"
    ));
}
