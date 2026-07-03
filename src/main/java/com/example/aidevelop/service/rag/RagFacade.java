package com.example.aidevelop.service.rag;

import com.example.aidevelop.config.RagProperties;
import com.example.aidevelop.model.rag.RagDocumentResult;
import com.example.aidevelop.model.rag.RagRequest;
import com.example.aidevelop.model.rag.RagRetrievalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * RAG 生产入口。Chat、Agent 和调试链路都应通过该 Facade 获取检索证据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagFacade {

    private final RagProperties ragProperties;
    private final RagPipelineService ragPipelineService;

    public RagRetrievalResult retrieve(RagRequest request) {
        if (request == null || !StringUtils.hasText(request.query())) {
            return RagRetrievalResult.disabled("", "RAG 查询为空");
        }
        if (!ragProperties.isEnabled()) {
            return RagRetrievalResult.disabled(request.query(), "RAG 功能未启用");
        }

        long startedAt = System.currentTimeMillis();
        try {
            RagPipelineService.PipelineResult pipelineResult = ragPipelineService.search(
                request.query(),
                request.conversationId(),
                request.topK()
            );
            List<RagDocumentResult> documents = pipelineResult.getDocuments() == null
                ? List.of()
                : pipelineResult.getDocuments().stream()
                    .filter(document -> matchesType(document, request.documentType()))
                    .map(this::toDocumentResult)
                    .limit(request.topK())
                    .toList();
            boolean empty = documents.isEmpty();
            String reason = empty ? "未检索到可用知识库证据" : "检索成功";
            log.info("RAG Facade 完成: profile={}, strategy={}, documents={}, latency={}ms",
                request.profile(), pipelineResult.getStrategy(), documents.size(), System.currentTimeMillis() - startedAt);
            return new RagRetrievalResult(
                pipelineResult.getOriginalQuery(),
                pipelineResult.getRewrittenQuery(),
                pipelineResult.getExpandedQuery(),
                String.valueOf(pipelineResult.getStrategy()),
                pipelineResult.getTransformationSummary(),
                documents,
                empty,
                false,
                reason
            );
        } catch (Exception ex) {
            log.warn("RAG Facade 降级: profile={}, query={}, error={}",
                request.profile(), request.query(), ex.getMessage());
            return new RagRetrievalResult(
                request.query(),
                request.query(),
                request.query(),
                "DEGRADED",
                "RAG 检索失败: " + ex.getMessage(),
                List.of(),
                true,
                true,
                ex.getMessage()
            );
        }
    }

    private RagDocumentResult toDocumentResult(Document document) {
        return new RagDocumentResult(document.getText(), document.getMetadata(), document.getScore());
    }

    private boolean matchesType(Document document, String documentType) {
        if (!StringUtils.hasText(documentType)) {
            return true;
        }
        Object type = document.getMetadata().get("type");
        return documentType.equals(String.valueOf(type));
    }
}
