package com.example.aidevelop.eval;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生成 RAG 离线评估 Markdown 摘要。
 */
public final class RagasEvalReportWriter {

    private RagasEvalReportWriter() {
    }

    public static void writeSummary(Path path, List<RagasEvalResult> results) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, render(results), StandardCharsets.UTF_8);
    }

    private static String render(List<RagasEvalResult> results) {
        List<RagasEvalResult> safeResults = results == null ? List.of() : results;
        StringBuilder builder = new StringBuilder();
        builder.append("# RAGAS-like Java 评估报告\n\n");
        builder.append("- 生成时间: ").append(LocalDateTime.now()).append("\n");
        builder.append("- 样本数: ").append(safeResults.size()).append("\n\n");

        builder.append("## 指标均值\n\n");
        builder.append("| metric | average |\n");
        builder.append("|---|---:|\n");
        appendAverage(builder, "context_precision", safeResults, result -> result.scores().contextPrecision());
        appendAverage(builder, "context_recall", safeResults, result -> result.scores().contextRecall());
        appendAverage(builder, "context_relevancy", safeResults, result -> result.scores().contextRelevancy());
        appendAverage(builder, "faithfulness", safeResults, result -> result.scores().faithfulness());
        appendAverage(builder, "answer_relevancy", safeResults, result -> result.scores().answerRelevancy());
        appendAverage(builder, "answer_correctness", safeResults, result -> result.scores().answerCorrectness());
        appendAverage(builder, "overall", safeResults, result -> result.scores().overall());

        builder.append("\n## 样本明细\n\n");
        builder.append("| id | overall | context_recall | context_precision | strategy | degraded |\n");
        builder.append("|---|---:|---:|---:|---|---|\n");
        for (RagasEvalResult result : safeResults) {
            builder.append("| ")
                .append(result.id())
                .append(" | ")
                .append(format(result.scores().overall()))
                .append(" | ")
                .append(format(result.scores().contextRecall()))
                .append(" | ")
                .append(format(result.scores().contextPrecision()))
                .append(" | ")
                .append(result.retrievalMetadata().getOrDefault("strategy", "unknown"))
                .append(" | ")
                .append(result.retrievalMetadata().getOrDefault("degraded", "unknown"))
                .append(" |\n");
        }
        return builder.toString();
    }

    private static void appendAverage(StringBuilder builder,
                                      String metric,
                                      List<RagasEvalResult> results,
                                      MetricExtractor extractor) {
        builder.append("| ")
            .append(metric)
            .append(" | ")
            .append(format(average(results, extractor)))
            .append(" |\n");
    }

    private static double average(List<RagasEvalResult> results, MetricExtractor extractor) {
        if (results.isEmpty()) {
            return 0.0;
        }
        return results.stream()
            .mapToDouble(extractor::value)
            .average()
            .orElse(0.0);
    }

    private static String format(double value) {
        return String.format("%.4f", value);
    }

    @FunctionalInterface
    private interface MetricExtractor {
        double value(RagasEvalResult result);
    }
}
