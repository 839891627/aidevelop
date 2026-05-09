package com.example.aidevelop.config;

import com.example.aidevelop.service.prompt.PromptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Map;
import java.util.function.Function;

/**
 * AI 模型核心配置：组装 ChatClient（LLM + 系统提示词 + Function Calling + RAG）
 */
@Slf4j
@Configuration
public class AiModelConfig {

    private final RagProperties ragProperties;
    private final PromptService promptService;
    private final Map<String, Function<?, ?>> aiFunctions;

    public AiModelConfig(RagProperties ragProperties,
                         PromptService promptService,
                         @AiFunction Map<String, Function<?, ?>> aiFunctions) {
        this.ragProperties = ragProperties;
        this.promptService = promptService;
        this.aiFunctions = aiFunctions;
    }

    /**
     * 构建 ChatClient Bean（仅 openai profile 生效）
     * 整合：系统提示词 + Function Calling（自动扫描 @AiFunction） + RAG Advisor
     */
    @Bean
    @Profile("openai")
    public ChatClient chatClientForOpenAI(@Qualifier("openAiChatModel") ChatModel chatModel,
                                           @Qualifier("vectorStore") ObjectProvider<VectorStore> vectorStoreProvider) {
        log.info("初始化 ChatClient，使用提供商: OpenAI (DeepSeek)");

        String[] functions = aiFunctions.keySet().toArray(new String[0]);
        log.info("自动注册 Function Calling 函数: {}", String.join(", ", functions));

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(promptService.getSystemPrompt())
            .defaultFunctions(functions);

        // 条件装配 RAG：启用时注入 QuestionAnswerAdvisor 实现检索增强
        if (ragProperties.isEnabled()) {
            VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
            if (vectorStore != null) {
                SearchRequest searchRequest = SearchRequest.builder()
                    .topK(ragProperties.getTopK())
                    .similarityThreshold(ragProperties.getSimilarityThreshold())
                    .build();
                builder.defaultAdvisors(new QuestionAnswerAdvisor(vectorStore, searchRequest));
                log.info("RAG 检索已启用");
            } else {
                log.warn("RAG 已配置启用，但未找到 VectorStore Bean，将跳过检索增强");
            }
        } else {
            log.info("RAG 检索已禁用，仅使用 DeepSeek 聊天");
        }

        return builder.build();
    }

}