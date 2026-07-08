package com.example.aidevelop.eval;

import com.example.aidevelop.model.rag.RagDocumentResult;
import com.example.aidevelop.model.rag.RagProfile;
import com.example.aidevelop.model.rag.RagRequest;
import com.example.aidevelop.model.rag.RagRetrievalResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RagasEvaluationExporterTest {

    @Test
    void shouldExportProductionRagRetrievalResultForEvaluation() {
        RagRetrievalResult retrievalResult = new RagRetrievalResult(
            "黄金客户利率是多少？",
            "黄金客户贷款利率",
            "黄金客户贷款利率 基准利率",
            "VECTOR_WITH_RERANK",
            "rewrite + expansion + rerank",
            List.of(new RagDocumentResult(
                "黄金客户：基准利率下浮 10% (即 3.915%)。",
                Map.of("filename", "loan_business_rules.txt"),
                0.91
            )),
            false,
            false,
            "检索成功"
        );
        List<RagRequest> capturedRequests = new ArrayList<>();
        RagasEvalSample sample = new RagasEvalSample(
            "loan_001",
            "黄金客户利率是多少？",
            "黄金客户利率为基准利率下浮 10%，即 3.915%。",
            List.of("黄金客户：基准利率下浮 10% (即 3.915%)。")
        );

        RagasEvalResult result = new RagasEvaluationExporter(
            request -> {
                capturedRequests.add(request);
                return retrievalResult;
            },
            new RagasLikeMetricCalculator()
        ).export(sample, "黄金客户利率为 3.915%。", 5, 0.2);

        assertEquals(1, capturedRequests.size());
        assertEquals("黄金客户利率是多少？", capturedRequests.get(0).query());
        assertEquals(5, capturedRequests.get(0).topK());
        assertEquals(0.2, capturedRequests.get(0).similarityThreshold());
        assertEquals(RagProfile.DEBUG, capturedRequests.get(0).profile());

        assertEquals("loan_001", result.id());
        assertEquals(List.of("黄金客户：基准利率下浮 10% (即 3.915%)。"), result.contexts());
        assertEquals("VECTOR_WITH_RERANK", result.retrievalMetadata().get("strategy"));
        assertFalse((Boolean) result.retrievalMetadata().get("degraded"));
    }
}
