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

@Slf4j
@Configuration
public class AiModelConfig {

    private final RagProperties ragProperties;
    private final PromptService promptService;

    public AiModelConfig(RagProperties ragProperties, PromptService promptService) {
        this.ragProperties = ragProperties;
        this.promptService = promptService;
    }

    /**
     * OpenAI 配置 - 当 profile 为 openai 时使用
     */
    @Bean
    @Profile("openai")
    public ChatClient chatClientForOpenAI(@Qualifier("openAiChatModel") ChatModel chatModel,
                                           @Qualifier("vectorStore") ObjectProvider<VectorStore> vectorStoreProvider) {
        log.info("初始化 ChatClient，使用提供商: OpenAI (DeepSeek)");

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(promptService.getSystemPrompt())
            .defaultFunctions("loanQueryFunction", "repaymentQueryFunction", "riskAssessmentFunction");

        if (ragProperties.isEnabled()) {
            VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
            if (vectorStore != null) {
                SearchRequest searchRequest = SearchRequest.defaults()
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