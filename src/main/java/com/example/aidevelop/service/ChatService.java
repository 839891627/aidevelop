package com.example.aidevelop.service;

import com.example.aidevelop.model.dto.chat.ChatRequest;
import com.example.aidevelop.model.dto.chat.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * 聊天服务接口
 */
public interface ChatService {

    /**
     * 普通聊天（阻塞式）
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 流式聊天（Server-Sent Events）
     */
    Flux<String> streamChat(ChatRequest request);

    /**
     * 清空对话历史
     */
    void clearConversation(String conversationId);
}
