package com.example.aidevelop.eval;

import com.example.aidevelop.model.rag.RagRetrievalResult;
import com.example.aidevelop.service.rag.RagContextFormatter;
import com.example.aidevelop.service.rag.RagFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * eval profile 下运行的离线 RAG 评估入口。
 */
@Slf4j
@Component
@Profile("eval")
public class RagasEvaluationRunner implements CommandLineRunner {

    private final RagFacade ragFacade;
    private final RagContextFormatter ragContextFormatter;

    @Autowired(required = false)
    @Qualifier("chatClientForOpenAI")
    private ChatClient chatClient;

    @Value("${app.ragas.dataset-path:eval/ragas/datasets/financial_rag_eval.jsonl}")
    private String datasetPath;

    @Value("${app.ragas.output-jsonl:eval/ragas/output/questions_answers_contexts.jsonl}")
    private String outputJsonlPath;

    @Value("${app.ragas.output-summary:eval/ragas/output/summary.md}")
    private String outputSummaryPath;

    @Value("${app.ragas.top-k:5}")
    private int topK;

    @Value("${app.ragas.similarity-threshold:0.2}")
    private double similarityThreshold;

    @Value("${app.ragas.generate-answer:true}")
    private boolean generateAnswer;

    public RagasEvaluationRunner(RagFacade ragFacade, RagContextFormatter ragContextFormatter) {
        this.ragFacade = ragFacade;
        this.ragContextFormatter = ragContextFormatter;
    }

    @Override
    public void run(String... args) throws Exception {
        Path dataset = Path.of(datasetPath);
        List<RagasEvalSample> samples = RagasEvalJsonlSupport.readSamples(dataset);
        RagasEvaluationExporter exporter = new RagasEvaluationExporter(
            ragFacade,
            new RagasLikeMetricCalculator()
        );

        List<RagasEvalResult> results = new ArrayList<>();
        for (RagasEvalSample sample : samples) {
            results.add(exporter.export(sample, topK, similarityThreshold, retrievalResult ->
                generateAnswer ? generateAnswer(sample, retrievalResult) : ""
            ));
        }

        Path jsonlOutput = Path.of(outputJsonlPath);
        Path summaryOutput = Path.of(outputSummaryPath);
        RagasEvalJsonlSupport.writeResults(jsonlOutput, results);
        RagasEvalReportWriter.writeSummary(summaryOutput, results);
        log.info("RAGAS-like Java 评估完成: samples={}, jsonl={}, summary={}",
            results.size(), jsonlOutput.toAbsolutePath(), summaryOutput.toAbsolutePath());
    }

    private String generateAnswer(RagasEvalSample sample, RagRetrievalResult retrievalResult) {
        if (chatClient == null) {
            throw new IllegalStateException("app.ragas.generate-answer=true 时需要 chatClientForOpenAI Bean，请使用 openai,eval profiles");
        }
        String ragContext = ragContextFormatter.formatForPrompt(retrievalResult);
        String prompt = """
            【问题】
            %s

            【知识库证据】
            %s

            请只基于知识库证据回答问题。若证据不足，请明确说明证据不足，不要编造。
            """.formatted(sample.question(), ragContext);

        String answer = chatClient.prompt()
            .system("你是金融信贷知识库问答评估助手，回答必须简洁、准确、忠于证据。")
            .user(prompt)
            .call()
            .chatResponse()
            .getResult()
            .getOutput()
            .getText();
        return StringUtils.hasText(answer) ? answer.trim() : "";
    }
}
