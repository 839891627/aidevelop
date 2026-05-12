package com.example.aidevelop.controller;

import com.example.aidevelop.model.dto.chat.ChatRequest;
import com.example.aidevelop.model.dto.chat.ChatResponse;
import com.example.aidevelop.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 聊天控制器 - 提供对话 API。
 * 说明：此控制器走 ChatClient 内置 Advisor 链路（可按配置启用基础 RAG）。
 * 若要体验可编排的高级 RAG 检索能力，请使用 /api/rag 下的接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "聊天接口", description = "AI 对话相关的 API")
public class ChatController {

    private final ChatService chatService;

    /**
     * 普通聊天接口（阻塞式）
     * POST /api/chat
     */
    @PostMapping
    @Operation(
            summary = "普通聊天",
            description = "发送消息给 AI 并获取完整响应（阻塞式），支持多轮对话。可通过请求参数覆盖模型、温度和最大输出 token。"
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
            description = "发送消息给 AI 并获取流式响应（Server-Sent Events），实时逐字返回。可通过请求参数覆盖模型、温度和最大输出 token。"
    )
    public SseEmitter streamChat(@Valid @RequestBody ChatRequest request) {
        log.info("收到流式聊天请求: {}", request.getMessage());
        return chatService.streamChat(request);
    }

    /**
     * 清空对话历史
     * DELETE /api/chat/{conversationIa}
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
}
