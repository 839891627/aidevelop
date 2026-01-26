package com.example.aidevelop.controller;

import com.example.aidevelop.config.RagProperties;
import com.example.aidevelop.model.dto.chat.ChatRequest;
import com.example.aidevelop.model.dto.chat.ChatResponse;
import com.example.aidevelop.model.dto.rag.QueryExpansionDetailDTO;
import com.example.aidevelop.model.dto.rag.QueryRewriteDetailDTO;
import com.example.aidevelop.model.dto.rag.SearchResultDTO;
import com.example.aidevelop.service.ChatService;
import com.example.aidevelop.service.rag.QueryExpansionService;
import com.example.aidevelop.service.rag.QueryRewriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天控制器 - 提供聊天相关的 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "聊天接口", description = "AI 对话相关的 API")
public class ChatController {

    private final ChatService chatService;
    private final VectorStore vectorStore;
    private final RagProperties ragProperties;
    private final QueryExpansionService queryExpansionService;
    private final QueryRewriteService queryRewriteService;

    /**
     * 调试接口：测试特定查询的相似度分数
     * GET /api/chat/test-search?query=逾期
     */
    @GetMapping("/test-search")
    @Operation(summary = "测试检索相似度", description = "返回所有文档及其与查询词的相似度分数")
    public List<Map<String, Object>> testSearch(
            @RequestParam String query
    ) {
        // 获取所有文档，不设置相似度阈值
        SearchRequest searchRequest = SearchRequest.query(query)
                .withTopK(ragProperties.getTopK())
                .withSimilarityThreshold(ragProperties.getSimilarityThreshold()); // 无阈值

        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        log.info("查询 '{}' 检索到 {} 个文档", query, documents.size());

        // 返回详细信息
        return documents.stream()
                .map(doc -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("score", doc.getScore());
                    info.put("type", doc.getMetadata().get("type"));
                    info.put("filename", doc.getMetadata().get("filename"));
                    info.put("content", doc.getContent());
                    info.put("contentLength", doc.getContent().length());
                    info.put("containsQuery", doc.getContent().contains(query));
                    return info;
                })
                .sorted((a, b) -> ((Double) b.get("score")).compareTo((Double) a.get("score"))) // 按分数降序
                .collect(Collectors.toList());
    }

    /**
     * 调试接口：检查向量库状态
     * GET /api/chat/debug
     */
    @GetMapping("/debug")
    @Operation(summary = "向量库调试信息", description = "检查向量库中有多少文档")
    public Map<String, Object> debug() {
        Map<String, Object> debugInfo = new HashMap<>();

        // 尝试获取所有文档（无阈值限制）
        SearchRequest allDocsRequest = SearchRequest.query(".")
                .withTopK(ragProperties.getTopK())
                .withSimilarityThreshold(ragProperties.getSimilarityThreshold());

        List<Document> allDocs = vectorStore.similaritySearch(allDocsRequest);

        debugInfo.put("totalDocuments", allDocs.size());
        debugInfo.put("vectorStoreType", vectorStore.getClass().getSimpleName());

        // 统计各类型的文档数量
        Map<String, Long> typeStats = allDocs.stream()
                .collect(Collectors.groupingBy(
                        doc -> doc.getMetadata().getOrDefault("type", "unknown").toString(),
                        Collectors.counting()
                ));
        debugInfo.put("typeStatistics", typeStats);

        // 显示所有文档的详细内容和长度
        List<Map<String, Object>> details = allDocs.stream()
                .map(doc -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("type", doc.getMetadata().get("type"));
                    detail.put("filename", doc.getMetadata().get("filename"));
                    detail.put("contentLength", doc.getContent().length());
                    detail.put("content", doc.getContent()); // 显示完整内容
                    detail.put("containsOverdue", doc.getContent().contains("逾期")); // 检查是否包含'逾期'
                    return detail;
                })
                .collect(Collectors.toList());
        debugInfo.put("documentDetails", details);

        return debugInfo;
    }

    /**
     * 普通聊天接口（阻塞式）
     * POST /api/chat
     */
    @PostMapping
    @Operation(
            summary = "普通聊天",
            description = "发送消息给 AI 并获取完整响应（阻塞式），支持多轮对话"
    )
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        log.info("收到聊天请求: {}", request.getMessage());
        return chatService.chat(request);
    }

    /**
     * 流式聊天接口（Server-Sent Events）
     * POST /api/chat/stream
     * Content-Type: text/event-stream
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "流式聊天",
            description = "发送消息给 AI 并获取流式响应（Server-Sent Events），实时逐字返回"
    )
    public Flux<String> streamChat(@Valid @RequestBody ChatRequest request) {
        log.info("收到流式聊天请求: {}", request.getMessage());
        return chatService.streamChat(request);
    }

    /**
     * 清空对话历史
     * DELETE /api/chat/{conversationId}
     */
    @DeleteMapping("/{conversationId}")
    @Operation(
            summary = "清空对话历史",
            description = "根据对话 ID 清空该对话的所有历史消息"
    )
    public void clearConversation(
            @Parameter(description = "对话 ID", required = true)
            @PathVariable String conversationId
    ) {
        log.info("清空对话历史: {}", conversationId);
        chatService.clearConversation(conversationId);
    }

    /**
     * 知识库检索接口（元数据过滤）
     * GET /api/chat/search
     */
    @GetMapping("/search")
    @Operation(
        summary = "知识库检索",
        description = "在向量库中检索相关文档，支持按类型过滤（规则/产品/风控）"
    )
    public List<SearchResultDTO> search(
        @Parameter(description = "检索关键词", required = true, example = "逾期处理")
        @RequestParam String query,
        @Parameter(description = "文档类型（规则/产品/风控）", required = false)
        @RequestParam(required = false) String type,
        @Parameter(description = "返回数量", required = false, example = "5")
        @RequestParam(defaultValue = "5") int topK
    ) {
        log.info("知识库检索 - query: {}, type: {}, topK: {}", query, type, topK);

        // ========== 查询扩展 ==========
        String expandedQuery = queryExpansionService.expandQuery(query);
        log.info("查询扩展: {} -> {}", query, expandedQuery);

        // SimpleVectorStore 不支持元数据过滤，需要手动实现
        // 获取更多结果以便后续过滤
        int searchTopK = type != null && !type.isEmpty() ? topK * 5 : topK;

        // 构建检索请求（使用扩展后的查询）
        SearchRequest searchRequest = SearchRequest.query(expandedQuery)
                .withTopK(searchTopK)
                .withSimilarityThreshold(ragProperties.getSimilarityThreshold());

        // 执行检索
        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        log.info("检索到 {} 个文档片段（过滤前）", documents.size());

        // 打印前3个文档的元数据和分数（调试用）
        documents.stream().limit(3).forEach(doc -> {
            log.debug("文档片段: score={}, type={}, content={}",
                doc.getScore(),
                doc.getMetadata().get("type"),
                doc.getContent().substring(0, Math.min(50, doc.getContent().length())));
        });

        // 手动过滤元数据
        if (type != null && !type.isEmpty()) {
            documents = documents.stream()
                    .filter(doc -> {
                        Object docType = doc.getMetadata().get("type");
                        boolean matches = docType != null && docType.toString().equals(type);
                        log.debug("文档类型过滤: type={}, matches={}", docType, matches);
                        return matches;
                    })
                    .limit(topK)
                    .collect(Collectors.toList());

            log.info("类型 '{}' 过滤后剩余 {} 个文档", type, documents.size());
        }

        // 转换为 DTO
        return documents.stream()
                .map(doc -> new SearchResultDTO(
                        doc.getContent(),
                        doc.getMetadata(),
                        doc.getScore()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 查询扩展调试接口
     * GET /api/chat/query-expansion?query=黑名单
     */
    @GetMapping("/query-expansion")
    @Operation(
            summary = "查询扩展调试",
            description = "查看查询扩展的详细信息，包括匹配的同义词和专业术语"
    )
    public QueryExpansionDetailDTO queryExpansionDebug(
            @Parameter(description = "需要分析的查询", required = true, example = "黑名单规则")
            @RequestParam String query
    ) {
        log.info("查询扩展调试 - query: {}", query);

        var detail = queryExpansionService.getExpansionDetail(query);

        return new QueryExpansionDetailDTO(
                detail.getOriginalQuery(),
                detail.getExpandedQuery(),
                detail.getSynonymExpansions(),
                detail.getTechnicalExpansions()
        );
    }

    /**
     * 查询重写调试接口
     * GET /api/chat/query-rewrite?query=它支持提前还款吗&conversationId=xxx
     */
    @GetMapping("/query-rewrite")
    @Operation(
            summary = "查询重写调试",
            description = "查看查询重写的详细信息，包括指代消解和上下文补全"
    )
    public QueryRewriteDetailDTO queryRewriteDebug(
            @Parameter(description = "需要重写的查询", required = true, example = "它支持提前还款吗")
            @RequestParam String query,
            @Parameter(description = "对话 ID（可选），用于获取历史上下文", required = false)
            @RequestParam(required = false) String conversationId
    ) {
        log.info("查询重写调试 - query: {}, conversationId: {}", query, conversationId);

        var detail = queryRewriteService.getRewriteDetail(query, conversationId);

        return new QueryRewriteDetailDTO(
                detail.getOriginalQuery(),
                detail.getRewrittenQuery(),
                detail.getChanged(),
                detail.getReason()
        );
    }
}
