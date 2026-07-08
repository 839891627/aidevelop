package com.example.aidevelop.eval;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagasEvalJsonlSupportTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadFinancialRagEvalSamplesFromJsonl() throws Exception {
        Path dataset = tempDir.resolve("financial_rag_eval.jsonl");
        Files.writeString(dataset, """
            {"id":"loan_001","question":"黄金客户利率是多少？","ground_truth":"黄金客户利率为基准利率下浮 10%，即 3.915%。","reference_contexts":["黄金客户：基准利率下浮 10% (即 3.915%)。"]}
            """);

        List<RagasEvalSample> samples = RagasEvalJsonlSupport.readSamples(dataset);

        assertEquals(1, samples.size());
        assertEquals("loan_001", samples.get(0).id());
        assertEquals("黄金客户利率是多少？", samples.get(0).question());
        assertEquals(List.of("黄金客户：基准利率下浮 10% (即 3.915%)。"), samples.get(0).referenceContexts());
    }

    @Test
    void shouldWriteEvaluationResultsAsJsonl() throws Exception {
        Path output = tempDir.resolve("questions_answers_contexts.jsonl");
        RagasEvalScores scores = new RagasEvalScores(1.0, 1.0, 0.8, 0.9, 0.7, 0.95, 0.89);
        RagasEvalResult result = new RagasEvalResult(
            "loan_001",
            "黄金客户利率是多少？",
            "黄金客户利率为 3.915%。",
            List.of("黄金客户：基准利率下浮 10% (即 3.915%)。"),
            "黄金客户利率为基准利率下浮 10%，即 3.915%。",
            List.of("黄金客户：基准利率下浮 10% (即 3.915%)。"),
            scores,
            Map.of("strategy", "VECTOR_WITH_RERANK", "degraded", false)
        );

        RagasEvalJsonlSupport.writeResults(output, List.of(result));

        String jsonl = Files.readString(output);
        assertTrue(jsonl.contains("\"question\":\"黄金客户利率是多少？\""));
        assertTrue(jsonl.contains("\"contexts\":[\"黄金客户：基准利率下浮 10% (即 3.915%)。\"]"));
        assertTrue(jsonl.contains("\"overall\":0.89"));
    }
}
