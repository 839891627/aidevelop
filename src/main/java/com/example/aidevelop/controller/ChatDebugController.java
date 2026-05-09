package com.example.aidevelop.controller;

import com.example.aidevelop.config.RagProperties;
import com.example.aidevelop.model.dto.rag.QueryExpansionDetailDTO;
import com.example.aidevelop.model.dto.rag.QueryRewriteDetailDTO;
import com.example.aidevelop.service.rag.QueryExpansionService;
import com.example.aidevelop.service.rag.QueryRewriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天调试控制器（仅开发环境）
 * 统一收敛调试接口，避免生产环境暴露调试能力。
 */
@Slf4j
@RestController
@Profile("dev")
@RequestMapping("/api/chat/debug")
@RequiredArgsConstructor
@Tag(name = "聊天调试接口", description = "仅开发环境启用的调试 API")
public class ChatDebugController {

    private final VectorStore vectorStore;
    private final RagProperties ragProperties;
    private final QueryExpansionService queryExpansionService;
    private final QueryRewriteService queryRewriteService;

    @GetMapping("/test-search")
    @Operation(summary = "测试检索相似度", description = "返回检索分数和元数据，可选返回文档片段")
    public List<SearchDebugItem> testSearch(
            @Parameter(description = "查询词", required = true)
            @RequestParam String query,
            @Parameter(description = "返回文档内容片段", required = false)
            @RequestParam(defaultValue = "false") boolean includeContent
    ) {
        SearchRequest searchRequest = SearchRequest.query(query)
                .withTopK(ragProperties.getTopK())
                .withSimilarityThreshold(ragProperties.getSimilarityThreshold());

        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        log.info("调试检索 - query: {}, docs: {}", query, documents.size());

        return documents.stream()
                .map(doc -> new SearchDebugItem(
                        doc.getScore(),
                        doc.getMetadata().get("type"),
                        doc.getMetadata().get("filename"),
                        doc.getContent().length(),
                        doc.getContent().contains(query),
                        includeContent ? shortenContent(doc.getContent()) : null
                ))
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .toList();
    }

    @GetMapping("/vector-store")
    @Operation(summary = "向量库状态", description = "返回向量库文档数量和类型分布，不返回完整文档内容")
    public VectorStoreDebugSummary vectorStoreDebug() {
        SearchRequest allDocsRequest = SearchRequest.query(".")
                .withTopK(ragProperties.getTopK())
                .withSimilarityThreshold(ragProperties.getSimilarityThreshold());

        List<Document> allDocs = vectorStore.similaritySearch(allDocsRequest);

        Map<String, Long> typeStats = allDocs.stream()
                .collect(Collectors.groupingBy(
                        doc -> doc.getMetadata().getOrDefault("type", "unknown").toString(),
                        Collectors.counting()
                ));

        List<DocumentBrief> samples = allDocs.stream()
                .limit(20)
                .map(doc -> new DocumentBrief(
                        doc.getMetadata().get("type"),
                        doc.getMetadata().get("filename"),
                        doc.getContent().length()
                ))
                .toList();

        return new VectorStoreDebugSummary(
                allDocs.size(),
                vectorStore.getClass().getSimpleName(),
                typeStats,
                samples
        );
    }

    @GetMapping("/query-expansion")
    @Operation(summary = "查询扩展调试", description = "查看查询扩展详情")
    public QueryExpansionDetailDTO queryExpansionDebug(
            @Parameter(description = "待分析查询", required = true)
            @RequestParam String query
    ) {
        var detail = queryExpansionService.getExpansionDetail(query);
        return new QueryExpansionDetailDTO(
                detail.getOriginalQuery(),
                detail.getExpandedQuery(),
                detail.getSynonymExpansions(),
                detail.getTechnicalExpansions()
        );
    }

    @GetMapping("/query-rewrite")
    @Operation(summary = "查询重写调试", description = "查看查询重写详情")
    public QueryRewriteDetailDTO queryRewriteDebug(
            @Parameter(description = "待重写查询", required = true)
            @RequestParam String query,
            @Parameter(description = "对话 ID（可选）", required = false)
            @RequestParam(required = false) String conversationId
    ) {
        var detail = queryRewriteService.getRewriteDetail(query, conversationId);
        return new QueryRewriteDetailDTO(
                detail.getOriginalQuery(),
                detail.getRewrittenQuery(),
                detail.getChanged(),
                detail.getReason()
        );
    }

    private String shortenContent(String content) {
        int maxLength = 200;
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    public record SearchDebugItem(
            double score,
            Object type,
            Object filename,
            int contentLength,
            boolean containsQuery,
            String contentPreview
    ) {
    }

    public record DocumentBrief(
            Object type,
            Object filename,
            int contentLength
    ) {
    }

    public record VectorStoreDebugSummary(
            int totalDocuments,
            String vectorStoreType,
            Map<String, Long> typeStatistics,
            List<DocumentBrief> documentSamples
    ) {
    }
}
