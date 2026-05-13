package com.example.aidevelop.agent.tool;

import com.example.aidevelop.model.dto.rag.PipelineSearchResultDTO;
import com.example.aidevelop.service.rag.RagOrchestrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RagSearchAgentTool implements AgentTool {

    private final RagOrchestrationService ragOrchestrationService;

    @Override
    public String name() {
        return "rag.search";
    }

    @Override
    public Object execute(Map<String, Object> args) {
        String query = readString(args, "query", "");
        String conversationId = readString(args, "conversationId", null);
        int topK = readInt(args, "topK", 5);
        PipelineSearchResultDTO result = ragOrchestrationService.pipelineSearch(query, conversationId, topK);
        return result;
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
