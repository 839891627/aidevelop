package com.example.aidevelop.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagasLikeMetricCalculatorTest {

    @Test
    void shouldScoreRetrievedContextsAndAnswerAgainstGoldenSample() {
        RagasEvalSample sample = new RagasEvalSample(
            "risk_001",
            "M2 阶段逾期应该怎么处理？",
            "M2 阶段为逾期 31-60 天，坐席人工介入电话催收，并按日利率 1.5 倍产生滞纳金。",
            List.of("M2 阶段 (31-60天)：坐席人工介入电话催收。产生滞纳金，费率为日利率的 1.5 倍。")
        );
        List<String> contexts = List.of(
            "M2 阶段 (31-60天)：坐席人工介入电话催收。产生滞纳金，费率为日利率的 1.5 倍。",
            "极速贷额度范围为 1,000 - 50,000 元。"
        );
        String answer = "M2 阶段是逾期 31-60 天，应由坐席人工电话催收，并产生日利率 1.5 倍的滞纳金。";

        RagasEvalScores scores = new RagasLikeMetricCalculator().score(sample, answer, contexts);

        assertEquals(1.0, scores.contextRecall());
        assertTrue(scores.contextPrecision() >= 0.5);
        assertTrue(scores.contextRelevancy() > 0.4);
        assertTrue(scores.faithfulness() > 0.6);
        assertTrue(scores.answerCorrectness() > 0.4);
        assertTrue(scores.overall() > 0.5);
    }

    @Test
    void shouldReturnZeroScoresWhenNoContextOrAnswerExists() {
        RagasEvalSample sample = new RagasEvalSample(
            "empty_001",
            "白金客户利率是多少？",
            "白金客户利率为 3.48%。",
            List.of("白金客户：基准利率下浮 20% (即 3.48%)。")
        );

        RagasEvalScores scores = new RagasLikeMetricCalculator().score(sample, "", List.of());

        assertEquals(0.0, scores.contextPrecision());
        assertEquals(0.0, scores.contextRecall());
        assertEquals(0.0, scores.contextRelevancy());
        assertEquals(0.0, scores.faithfulness());
        assertEquals(0.0, scores.answerRelevancy());
        assertEquals(0.0, scores.answerCorrectness());
        assertEquals(0.0, scores.overall());
    }
}
