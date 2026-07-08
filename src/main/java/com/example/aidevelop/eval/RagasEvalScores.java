package com.example.aidevelop.eval;

/**
 * Java-native RAGAS-like 指标结果，取值范围均为 0.0 到 1.0。
 */
public record RagasEvalScores(
    double contextPrecision,
    double contextRecall,
    double contextRelevancy,
    double faithfulness,
    double answerRelevancy,
    double answerCorrectness,
    double overall
) {
}
