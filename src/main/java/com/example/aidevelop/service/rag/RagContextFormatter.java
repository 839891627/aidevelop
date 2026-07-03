package com.example.aidevelop.service.rag;

import com.example.aidevelop.model.rag.RagDocumentResult;
import com.example.aidevelop.model.rag.RagRetrievalResult;
import org.springframework.stereotype.Component;

/**
 * 将统一 RAG 结果转换为可注入 Chat prompt 的证据上下文。
 */
@Component
public class RagContextFormatter {

    public String formatForPrompt(RagRetrievalResult result) {
        if (result == null || result.documents() == null || result.documents().isEmpty()) {
            return """
                当前知识库没有检索到可用证据。
                回答时必须明确说明知识库证据不足，不要编造规则、阈值或流程。
                """;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("以下是本次从知识库检索到的证据，请优先基于这些证据回答。\n");
        builder.append("检索策略: ").append(result.strategy()).append("\n");
        for (int index = 0; index < result.documents().size(); index++) {
            RagDocumentResult document = result.documents().get(index);
            builder.append("\n[证据").append(index + 1).append("]\n");
            Object filename = document.metadata() == null ? null : document.metadata().get("filename");
            Object type = document.metadata() == null ? null : document.metadata().get("type");
            if (filename != null || type != null) {
                builder.append("来源: ")
                    .append(filename == null ? "unknown" : filename)
                    .append(", 类型: ")
                    .append(type == null ? "unknown" : type)
                    .append("\n");
            }
            builder.append(document.content()).append("\n");
        }
        return builder.toString();
    }
}
