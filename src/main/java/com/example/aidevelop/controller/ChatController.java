package com.example.aidevelop.controller;

import com.example.aidevelop.model.dto.ChatRequest;
import com.example.aidevelop.model.dto.ChatResponse;
import com.example.aidevelop.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

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
        return chatService.streamChat(request)
            .map(chunk -> "data: " + chunk + "\n\n");  // SSE 格式
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
}
