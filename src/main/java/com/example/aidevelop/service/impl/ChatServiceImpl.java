package com.example.aidevelop.service.impl;

import com.example.aidevelop.exception.AiServiceException;
import com.example.aidevelop.model.dto.chat.ChatMode;
import com.example.aidevelop.model.dto.chat.ChatConversationSummary;
import com.example.aidevelop.model.dto.chat.ChatMessageResponse;
import com.example.aidevelop.model.dto.chat.ChatRequest;
import com.example.aidevelop.model.dto.chat.ChatResponse;
import com.example.aidevelop.model.entity.Conversation;
import com.example.aidevelop.model.entity.Message;
import com.example.aidevelop.model.entity.MessageRole;
import com.example.aidevelop.repository.ConversationRepository;
import com.example.aidevelop.service.ChatService;
import com.example.aidevelop.service.IntentRoutingService;
import com.example.aidevelop.service.prompt.PromptRegistryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    @Resource(name = "chatClientForOpenAI")
    private ChatClient chatClient;
    @Resource
    private ConversationRepository conversationRepository;
    @Resource
    private IntentRoutingService intentRoutingService;
    @Resource
    private PromptRegistryService promptRegistryService;
    @Resource
    private ObjectProvider<VectorStore> vectorStoreProvider;

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
            conversationRepository.save(conversation);

            // 3. 构建提示词（包含历史消息）
            String prompt = buildPromptWithHistory(conversation);
            OpenAiChatOptions runtimeOptions = buildRuntimeOptions(request);

            // 4. 根据前端选择的能力模式，决定本次请求使用哪套提示词、是否启用 RAG/工具。
            log.debug("发送请求到 AI 模型: {}", request.getMessage());
            ChatMode chatMode = ChatMode.from(request.getMode());
            IntentRoutingService.RoutePlan routePlan = resolveRoutePlan(chatMode, request.getMessage());
            var promptSpec = preparePromptSpec(chatMode, routePlan);
            if (runtimeOptions != null) {
                promptSpec = promptSpec.options(runtimeOptions);
            }
            org.springframework.ai.chat.model.ChatResponse aiResponse = promptSpec.user(prompt)
                    .call()
                    .chatResponse();

            String responseContent = aiResponse.getResult().getOutput().getText();

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
    public SseEmitter streamChat(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);

        try {
            // 获取或创建对话
            Conversation conversation = getOrCreateConversation(request.getConversationId());
            sendConversationMeta(emitter, conversation.getConversationId());

            // 添加用户消息
            Message userMessage = new Message(
                    UUID.randomUUID().toString(),
                    MessageRole.USER,
                    request.getMessage(),
                    LocalDateTime.now(),
                    null
            );
            conversation.addMessage(userMessage);
            conversationRepository.save(conversation);

            // 构建提示词
            String prompt = buildPromptWithHistory(conversation);
            OpenAiChatOptions runtimeOptions = buildRuntimeOptions(request);

            // 流式调用和阻塞调用共用同一套路由逻辑，保证两种接口行为一致。
            log.debug("开始流式响应: {}", request.getMessage());

            StringBuilder fullResponse = new StringBuilder();

            ChatMode chatMode = ChatMode.from(request.getMode());
            IntentRoutingService.RoutePlan routePlan = resolveRoutePlan(chatMode, request.getMessage());
            var promptSpec = preparePromptSpec(chatMode, routePlan);
            if (runtimeOptions != null) {
                promptSpec = promptSpec.options(runtimeOptions);
            }

            promptSpec.user(prompt)
                    .stream()
                    .content()
                    .doOnNext(chunk -> {
                        fullResponse.append(chunk);
                        sendSseChunk(emitter, chunk);
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
                        emitter.complete();
                        log.debug("流式响应完成，已保存对话历史");
                    })
                    .doOnError(error -> {
                        log.error("流式响应出错", error);
                        emitter.completeWithError(error);
                    })
                    .subscribe();

            return emitter;
        } catch (Exception e) {
            log.error("流式聊天启动失败", e);
            emitter.completeWithError(new AiServiceException("流式聊天启动失败: " + e.getMessage(), e));
            return emitter;
        }
    }

    @Override
    public void clearConversation(String conversationId) {
        conversationRepository.delete(conversationId);
        log.info("已清空对话历史: {}", conversationId);
    }

    @Override
    public List<ChatConversationSummary> listConversations() {
        return conversationRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        Conversation::getUpdatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .map(conversation -> ChatConversationSummary.builder()
                        .conversationId(conversation.getConversationId())
                        .title(resolveConversationTitle(conversation))
                        .messageCount(conversation.getMessages().size())
                        .createdAt(conversation.getCreatedAt())
                        .updatedAt(conversation.getUpdatedAt())
                        .build())
                .toList();
    }

    @Override
    public List<ChatMessageResponse> getConversationMessages(String conversationId) {
        return conversationRepository.findById(conversationId)
                .map(conversation -> conversation.getMessages().stream()
                        .map(message -> ChatMessageResponse.builder()
                                .messageId(message.getId())
                                .role(message.getRole().name())
                                .content(message.getContent())
                                .model(message.getModel())
                                .createdAt(message.getTimestamp())
                                .build())
                        .toList())
                .orElseGet(List::of);
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

    private String resolveConversationTitle(Conversation conversation) {
        return conversation.getMessages().stream()
                .filter(message -> message.getRole() == MessageRole.USER)
                .map(Message::getContent)
                .filter(StringUtils::hasText)
                .findFirst()
                .map(this::truncateTitle)
                .orElse("未命名会话");
    }

    private String truncateTitle(String title) {
        String normalized = title.trim().replaceAll("\\s+", " ");
        int maxLength = 30;
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private void sendSseChunk(SseEmitter emitter, String chunk) {
        try {
            emitter.send(SseEmitter.event().data(chunk));
        } catch (IOException | IllegalStateException e) {
            throw new AiServiceException("流式数据发送失败", e);
        }
    }

    private void sendConversationMeta(SseEmitter emitter, String conversationId) {
        try {
            String escapedId = conversationId.replace("\"", "\\\"");
            emitter.send(SseEmitter.event()
                    .name("meta")
                    .data("{\"conversationId\":\"" + escapedId + "\"}"));
        } catch (IOException | IllegalStateException e) {
            throw new AiServiceException("流式元数据发送失败", e);
        }
    }

    private OpenAiChatOptions buildRuntimeOptions(ChatRequest request) {
        boolean hasRuntimeOptions = StringUtils.hasText(request.getModel())
                || request.getTemperature() != null
                || request.getMaxTokens() != null;
        if (!hasRuntimeOptions) {
            return null;
        }

        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder();
        if (StringUtils.hasText(request.getModel())) {
            builder.model(request.getModel().trim());
        }
        if (request.getTemperature() != null) {
            builder.temperature(request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            builder.maxTokens(request.getMaxTokens());
        }
        return builder.build();
    }

    private IntentRoutingService.RoutePlan resolveRoutePlan(ChatMode chatMode, String message) {
        return switch (chatMode) {
            // 常规聊天明确不走 IntentRoutingService，避免命中金融关键词后自动挂载 RAG/工具。
            case GENERAL -> null;
            // 金融 RAG 模式强制构造 RAG_ONLY 计划，不再依赖关键词判断。
            case FINANCIAL_RAG -> intentRoutingService.financialRagPlan();
            // auto 是兼容旧体验的自动路由：根据问题内容判断 Tool/RAG/Hybrid。
            case AUTO -> intentRoutingService.plan(message);
        };
    }

    private ChatClient.ChatClientRequestSpec preparePromptSpec(ChatMode chatMode, IntentRoutingService.RoutePlan routePlan) {
        if (routePlan == null) {
            log.debug("聊天模式: {}, 不启用 RAG/工具路由", chatMode);
        } else {
            log.debug("聊天模式: {}, 意图路由结果: type={}, rag={}, tools={}, reason={}",
                chatMode, routePlan.routeType(), routePlan.ragEnabled(), routePlan.allowedToolNames(), routePlan.reason());
        }

        ChatClient.ChatClientRequestSpec promptSpec = chatClient.prompt()
            .system(resolveSystemPrompt(chatMode));

        if (routePlan != null && !routePlan.allowedToolNames().isEmpty()) {
            // 只把路由计划允许的工具暴露给模型，降低误调用和越权调用风险。
            promptSpec = promptSpec.toolNames(routePlan.allowedToolNames().toArray(new String[0]));
        }

        if (routePlan != null && routePlan.ragEnabled()) {
            VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
            if (vectorStore != null) {
                // RAG 参数来自 RoutePlan，不同链路可以使用不同 topK 和相似度阈值。
                SearchRequest searchRequest = SearchRequest.builder()
                    .topK(routePlan.ragTopK())
                    .similarityThreshold(routePlan.ragSimilarityThreshold())
                    .build();
                promptSpec = promptSpec.advisors(QuestionAnswerAdvisor.builder(vectorStore)
                    .searchRequest(searchRequest)
                    .build());
            } else {
                log.warn("路由要求启用 RAG，但未找到 VectorStore，自动降级为非 RAG");
            }
        }

        return promptSpec;
    }

    private String resolveSystemPrompt(ChatMode chatMode) {
        return switch (chatMode) {
            // 三种模式使用不同 promptKey，让“常规聊天”和“金融 RAG”在系统提示词层面隔离。
            case GENERAL -> promptRegistryService.getGeneralChatPrompt();
            case FINANCIAL_RAG -> promptRegistryService.getFinancialRagPrompt();
            case AUTO -> promptRegistryService.getSystemPrompt();
        };
    }
}
