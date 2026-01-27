package com.example.aidevelop.service.cost;

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

    /**
     * 模型定价表（单位：元/千tokens）
     * DeepSeek: https://api.deepseek.com/pricing
     * 智谱AI: https://open.bigmodel.cn/pricing
     */
    private static final Map<String, ModelPricing> PRICING_TABLE = new HashMap<>();

    static {
        // DeepSeek 定价（2025年1月）
        // 输入: ¥1/百万tokens ≈ ¥0.001/千tokens
        // 输出: ¥2/百万tokens ≈ ¥0.002/千tokens
        PRICING_TABLE.put("deepseek-chat", new ModelPricing(
            new BigDecimal("0.001"),  // 输入定价
            new BigDecimal("0.002")   // 输出定价
        ));

        // 智谱AI Embedding 定价
        // embedding-2: ¥0.0007/千tokens
        PRICING_TABLE.put("embedding-2", new ModelPricing(
            new BigDecimal("0.0007"),  // 只有输入定价
            new BigDecimal("0.0007")   // 输出与输入相同
        ));

        // 可以继续添加其他模型...
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
        ModelPricing pricing = PRICING_TABLE.get(modelName);

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
        return PRICING_TABLE.get(modelName);
    }

    /**
     * 检查模型是否在定价表中
     */
    public boolean isModelSupported(String modelName) {
        return PRICING_TABLE.containsKey(modelName);
    }

    /**
     * 模型定价记录
     */
    public record ModelPricing(
        BigDecimal inputPrice,   // 输入定价（元/千tokens）
        BigDecimal outputPrice   // 输出定价（元/千tokens）
    ) {}
}
