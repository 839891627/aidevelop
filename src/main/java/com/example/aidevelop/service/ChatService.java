package com.example.aidevelop.service;

import com.example.aidevelop.model.dto.chat.ChatRequest;
import com.example.aidevelop.model.dto.chat.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    SseEmitter streamChat(ChatRequest request);

    /**
     * 清空对话历史
     */
    void clearConversation(String conversationId);
}
