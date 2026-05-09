package com.example.aidevelop.service.cost;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 成本计算服务
 * 根据不同模型的定价计算成本
 */
@Component
public class AiCostCalculator {

    private final Map<String, ModelPricing> pricingTable = new HashMap<>();

    public AiCostCalculator(
        @Value("${spring.ai.openai.chat.options.model}") String chatModel,
        @Value("${spring.ai.ollama.embedding.options.model}") String embeddingModel,
        @Value("${app.ai.pricing.chat-input:0.001}") BigDecimal chatInputPrice,
        @Value("${app.ai.pricing.chat-output:0.002}") BigDecimal chatOutputPrice,
        @Value("${app.ai.pricing.embedding-input:0.0}") BigDecimal embeddingInputPrice
    ) {
        // 聊天模型定价（元/千tokens）
        pricingTable.put(chatModel, new ModelPricing(chatInputPrice, chatOutputPrice));
        // 向量模型定价（通常仅输入计费）
        pricingTable.put(embeddingModel, new ModelPricing(embeddingInputPrice, embeddingInputPrice));
    }

    /**
     * 计算成本
     *
     * @param modelName 模型名称
     * @param promptTokens 输入token数
     * @param completionTokens 输出token数
     * @return 成本（元）
     */
    public BigDecimal calculateCost(String modelName, int promptTokens, int completionTokens) {
        ModelPricing pricing = pricingTable.get(modelName);

        if (pricing == null) {
            // 未知模型，返回0
            return BigDecimal.ZERO;
        }

        // 计算输入成本：(输入token数 / 1000) * 输入定价
        BigDecimal inputCost = BigDecimal.valueOf(promptTokens)
            .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
            .multiply(pricing.inputPrice());

        // 计算输出成本：(输出token数 / 1000) * 输出定价
        BigDecimal outputCost = BigDecimal.valueOf(completionTokens)
            .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
            .multiply(pricing.outputPrice());

        // 总成本
        return inputCost.add(outputCost).setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * 估算成本（用于调用前）
     *
     * @param modelName 模型名称
     * @param estimatedTokens 估算的token数
     * @return 估算成本（元）
     */
    public BigDecimal estimateCost(String modelName, int estimatedTokens) {
        // 假设输入输出各占一半
        return calculateCost(modelName, estimatedTokens / 2, estimatedTokens / 2);
    }

    /**
     * 获取模型定价信息
     */
    public ModelPricing getModelPricing(String modelName) {
        return pricingTable.get(modelName);
    }

    /**
     * 检查模型是否在定价表中
     */
    public boolean isModelSupported(String modelName) {
        return pricingTable.containsKey(modelName);
    }

    /**
     * 模型定价记录
     */
    public record ModelPricing(
        BigDecimal inputPrice,   // 输入定价（元/千tokens）
        BigDecimal outputPrice   // 输出定价（元/千tokens）
    ) {}
}
