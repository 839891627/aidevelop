package com.example.aidevelop.eval;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RagasEvalReportWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldWriteMarkdownSummaryWithAveragesAndCaseRows() throws Exception {
        Path summary = tempDir.resolve("summary.md");
        RagasEvalResult result = new RagasEvalResult(
            "loan_001",
            "黄金客户利率是多少？",
            "黄金客户利率为 3.915%。",
            List.of("黄金客户：基准利率下浮 10% (即 3.915%)。"),
            "黄金客户利率为基准利率下浮 10%，即 3.915%。",
            List.of("黄金客户：基准利率下浮 10% (即 3.915%)。"),
            new RagasEvalScores(1.0, 1.0, 0.8, 0.9, 0.7, 0.95, 0.89),
            Map.of("strategy", "VECTOR_WITH_RERANK", "degraded", false)
        );

        RagasEvalReportWriter.writeSummary(summary, List.of(result));

        String markdown = Files.readString(summary);
        assertTrue(markdown.contains("# RAGAS-like Java 评估报告"));
        assertTrue(markdown.contains("| overall | 0.8900 |"));
        assertTrue(markdown.contains("| loan_001 | 0.8900 | 1.0000 | 1.0000 | VECTOR_WITH_RERANK | false |"));
    }
}
