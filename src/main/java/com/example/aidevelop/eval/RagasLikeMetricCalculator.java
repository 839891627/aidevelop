package com.example.aidevelop.eval;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Java-native 的 RAGAS-like 基线指标。
 * <p>
 * 这里使用确定性的文本覆盖率/相似度，适合作为 CI 回归基线；如需 LLM-as-judge，
 * 可在此基础上增加 Spring AI Evaluator 或专用 judge prompt。
 */
public class RagasLikeMetricCalculator {

    private static final double RELEVANT_CONTEXT_THRESHOLD = 0.12;

    public RagasEvalScores score(RagasEvalSample sample, String answer, List<String> contexts) {
        List<String> safeContexts = contexts == null ? List.of() : contexts;
        String safeAnswer = answer == null ? "" : answer;

        double contextPrecision = contextPrecision(sample, safeContexts);
        double contextRecall = contextRecall(sample.referenceContexts(), safeContexts);
        double contextRelevancy = contextRelevancy(sample, safeContexts);
        double faithfulness = faithfulness(safeAnswer, safeContexts);
        double answerRelevancy = answerRelevancy(sample.question(), safeAnswer, safeContexts);
        double answerCorrectness = similarity(sample.groundTruth(), safeAnswer);
        double overall = average(
            contextPrecision,
            contextRecall,
            contextRelevancy,
            faithfulness,
            answerRelevancy,
            answerCorrectness
        );

        return new RagasEvalScores(
            round(contextPrecision),
            round(contextRecall),
            round(contextRelevancy),
            round(faithfulness),
            round(answerRelevancy),
            round(answerCorrectness),
            round(overall)
        );
    }

    private double contextPrecision(RagasEvalSample sample, List<String> contexts) {
        if (contexts.isEmpty()) {
            return 0.0;
        }
        String relevanceTarget = join(List.of(sample.question(), sample.groundTruth(), join(sample.referenceContexts())));
        long relevant = contexts.stream()
            .filter(context -> similarity(context, relevanceTarget) >= RELEVANT_CONTEXT_THRESHOLD)
            .count();
        return (double) relevant / contexts.size();
    }

    private double contextRecall(List<String> referenceContexts, List<String> contexts) {
        if (referenceContexts == null || referenceContexts.isEmpty() || contexts.isEmpty()) {
            return 0.0;
        }
        String retrieved = join(contexts);
        long covered = referenceContexts.stream()
            .filter(reference -> containsNormalized(retrieved, reference) || similarity(reference, retrieved) >= RELEVANT_CONTEXT_THRESHOLD)
            .count();
        return (double) covered / referenceContexts.size();
    }

    private double contextRelevancy(RagasEvalSample sample, List<String> contexts) {
        if (contexts.isEmpty()) {
            return 0.0;
        }
        String relevanceTarget = join(List.of(sample.question(), sample.groundTruth(), join(sample.referenceContexts())));
        return contexts.stream()
            .mapToDouble(context -> similarity(relevanceTarget, context))
            .max()
            .orElse(0.0);
    }

    private double faithfulness(String answer, List<String> contexts) {
        if (!StringUtils.hasText(answer) || contexts.isEmpty()) {
            return 0.0;
        }
        String retrieved = join(contexts);
        return coverage(answer, retrieved);
    }

    private double answerRelevancy(String question, String answer, List<String> contexts) {
        if (!StringUtils.hasText(answer)) {
            return 0.0;
        }
        double questionSimilarity = similarity(question, answer);
        double contextSupport = contexts.isEmpty() ? 0.0 : coverage(answer, join(contexts));
        return Math.max(questionSimilarity, contextSupport);
    }

    private double similarity(String left, String right) {
        Set<String> leftTokens = tokens(left);
        Set<String> rightTokens = tokens(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(leftTokens);
        intersection.retainAll(rightTokens);
        Set<String> union = new HashSet<>(leftTokens);
        union.addAll(rightTokens);
        return (double) intersection.size() / union.size();
    }

    private double coverage(String expected, String actual) {
        Set<String> expectedTokens = tokens(expected);
        Set<String> actualTokens = tokens(actual);
        if (expectedTokens.isEmpty() || actualTokens.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(expectedTokens);
        intersection.retainAll(actualTokens);
        return (double) intersection.size() / expectedTokens.size();
    }

    private Set<String> tokens(String text) {
        String normalized = normalize(text);
        Set<String> tokens = new HashSet<>();
        if (!StringUtils.hasText(normalized)) {
            return tokens;
        }
        String[] words = normalized.split("\\s+");
        for (String word : words) {
            if (word.length() > 1) {
                tokens.add(word);
            }
        }
        String compact = normalized.replace(" ", "");
        for (int i = 0; i < compact.length(); i++) {
            tokens.add(compact.substring(i, i + 1));
        }
        for (int i = 0; i < compact.length() - 1; i++) {
            tokens.add(compact.substring(i, i + 2));
        }
        return tokens;
    }

    private boolean containsNormalized(String text, String expected) {
        String normalizedText = normalize(text).replace(" ", "");
        String normalizedExpected = normalize(expected).replace(" ", "");
        return StringUtils.hasText(normalizedExpected) && normalizedText.contains(normalizedExpected);
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text
            .toLowerCase()
            .replaceAll("[\\p{Punct}，。！？；：（）【】、|]+", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String join(List<String> values) {
        return values == null ? "" : String.join("\n", values);
    }

    private double average(double... values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return values.length == 0 ? 0.0 : sum / values.length;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
            .setScale(4, RoundingMode.HALF_UP)
            .doubleValue();
    }
}
