package com.example.aidevelop.service.impl;

import com.example.aidevelop.exception.AiServiceException;
import com.example.aidevelop.model.dto.chat.ChatRequest;
import com.example.aidevelop.model.dto.chat.ChatResponse;
import com.example.aidevelop.model.entity.Conversation;
import com.example.aidevelop.model.entity.Message;
import com.example.aidevelop.model.entity.MessageRole;
import com.example.aidevelop.repository.ConversationRepository;
import com.example.aidevelop.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final ConversationRepository conversationRepository;

    @Override
    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取或创建对话
            Conversation conversation = getOrCreateConversation(request.getConversationId());

            // 2. 添加用户消息到历史
            Message userMessage = new Message(
                UUID.randomUUID().toString(),
                MessageRole.USER,
                request.getMessage(),
                LocalDateTime.now(),
                null
            );
            conversation.addMessage(userMessage);

            // 3. 构建提示词（包含历史消息）
            String prompt = buildPromptWithHistory(conversation);

            // 4. 调用 AI 模型
            log.debug("发送请求到 AI 模型: {}", request.getMessage());
            org.springframework.ai.chat.model.ChatResponse aiResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .chatResponse();

            String responseContent = aiResponse.getResult().getOutput().getContent();

            // 5. 添加 AI 响应到历史
            Message assistantMessage = new Message(
                UUID.randomUUID().toString(),
                MessageRole.ASSISTANT,
                responseContent,
                LocalDateTime.now(),
                aiResponse.getMetadata().getModel()
            );
            conversation.addMessage(assistantMessage);

            // 6. 保存对话
            conversationRepository.save(conversation);

            long responseTime = System.currentTimeMillis() - startTime;

            return ChatResponse.builder()
                .conversationId(conversation.getConversationId())
                .message(responseContent)
                .model(aiResponse.getMetadata().getModel())
                .tokensUsed(Math.toIntExact(aiResponse.getMetadata().getUsage().getTotalTokens()))
                .responseTime(responseTime)
                .build();

        } catch (Exception e) {
            log.error("AI 服务调用失败", e);
            throw new AiServiceException("AI 服务调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> streamChat(ChatRequest request) {
        try {
            // 获取或创建对话
            Conversation conversation = getOrCreateConversation(request.getConversationId());

            // 添加用户消息
            Message userMessage = new Message(
                UUID.randomUUID().toString(),
                MessageRole.USER,
                request.getMessage(),
                LocalDateTime.now(),
                null
            );
            conversation.addMessage(userMessage);

            // 构建提示词
            String prompt = buildPromptWithHistory(conversation);

            // 流式调用
            log.debug("开始流式响应: {}", request.getMessage());

            StringBuilder fullResponse = new StringBuilder();

            return chatClient.prompt()
                .user(prompt)
                .stream()
                .content()
                .doOnNext(chunk -> {
                    fullResponse.append(chunk);
                    log.trace("接收到流式数据块: {}", chunk);
                })
                .doOnComplete(() -> {
                    // 流式响应完成后，保存完整响应到历史
                    Message assistantMessage = new Message(
                        UUID.randomUUID().toString(),
                        MessageRole.ASSISTANT,
                        fullResponse.toString(),
                        LocalDateTime.now(),
                        null
                    );
                    conversation.addMessage(assistantMessage);
                    conversationRepository.save(conversation);
                    log.debug("流式响应完成，已保存对话历史");
                })
                .doOnError(error -> {
                    log.error("流式响应出错", error);
                });

        } catch (Exception e) {
            log.error("流式聊天启动失败", e);
            return Flux.error(new AiServiceException("流式聊天启动失败: " + e.getMessage(), e));
        }
    }

    @Override
    public void clearConversation(String conversationId) {
        conversationRepository.delete(conversationId);
        log.info("已清空对话历史: {}", conversationId);
    }

    // ===== 私有辅助方法 =====

    private Conversation getOrCreateConversation(String conversationId) {
        if (conversationId != null && !conversationId.isEmpty()) {
            return conversationRepository.findById(conversationId)
                .orElseGet(() -> createNewConversation(conversationId));
        }
        return createNewConversation(null);
    }

    private Conversation createNewConversation(String conversationId) {
        Conversation conversation = new Conversation();
        conversation.setConversationId(
            conversationId != null ? conversationId : UUID.randomUUID().toString()
        );
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        return conversation;
    }

    /**
     * 构建包含历史消息的提示词
     * 格式：
     * 用户: xxx
     * 助手: xxx
     * 用户: xxx
     */
    private String buildPromptWithHistory(Conversation conversation) {
        if (conversation.getMessages().isEmpty()) {
            return "";
        }

        return conversation.getMessages().stream()
            .filter(m -> m.getRole() != MessageRole.SYSTEM)
            .map(m -> {
                String roleLabel = m.getRole() == MessageRole.USER ? "用户" : "助手";
                return roleLabel + ": " + m.getContent();
            })
            .collect(Collectors.joining("\n")) + "\n助手: ";
    }
}
