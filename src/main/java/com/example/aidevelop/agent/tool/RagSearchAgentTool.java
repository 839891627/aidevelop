package com.example.aidevelop.agent.tool;

import com.example.aidevelop.model.rag.RagDocumentResult;
import com.example.aidevelop.model.rag.RagProfile;
import com.example.aidevelop.model.rag.RagRequest;
import com.example.aidevelop.model.rag.RagRetrievalResult;
import com.example.aidevelop.model.dto.rag.PipelineSearchResultDTO;
import com.example.aidevelop.service.rag.RagFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RagSearchAgentTool implements AgentTool {

    private final RagFacade ragFacade;

    @Override
    public String name() {
        return "rag.search";
    }

    @Override
    public String description() {
        return "rag.search: 知识库语义检索。参数: query(string,检索问题), conversationId(string,可选,会话ID), topK(int,可选,返回条数,默认5)";
    }

    @Override
    public Object execute(Map<String, Object> args) {
        String query = readString(args, "query", "");
        String conversationId = readString(args, "conversationId", null);
        int topK = readInt(args, "topK", 5);
        RagRetrievalResult result = ragFacade.retrieve(new RagRequest(
            query,
            conversationId,
            topK,
            0.2,
            RagProfile.AGENT,
            null
        ));
        List<PipelineSearchResultDTO.DocumentResult> documents = result.documents().stream()
            .map(this::toDocumentResult)
            .toList();
        return new PipelineSearchResultDTO(
            result.originalQuery(),
            result.rewrittenQuery(),
            result.expandedQuery(),
            result.strategy(),
            result.transformationSummary(),
            documents
        );
    }

    private PipelineSearchResultDTO.DocumentResult toDocumentResult(RagDocumentResult document) {
        return new PipelineSearchResultDTO.DocumentResult(
            document.content(),
            document.metadata(),
            document.score()
        );
    }

    private String readString(Map<String, Object> args, String key, String defaultValue) {
        if (args == null) {
            return defaultValue;
        }
        Object value = args.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private int readInt(Map<String, Object> args, String key, int defaultValue) {
        if (args == null) {
            return defaultValue;
        }
        Object value = args.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
