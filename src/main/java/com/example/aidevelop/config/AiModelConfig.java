package com.example.aidevelop.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class AiModelConfig {

    /**
     * OpenAI 配置 - 当 profile 为 openai 时使用
     */
    @Bean
    @Profile("openai")
    public ChatClient chatClientForOpenAI(@Qualifier("openAiChatModel") ChatModel chatModel) {
        log.info("初始化 ChatClient，使用提供商: OpenAI");
        return ChatClient.builder(chatModel)
            .defaultSystem("你是一个专业的 AI 助手，请用中文回答问题。")
            .build();
    }

    /**
     * Anthropic 配置 - 当 profile 为 anthropic 时使用
     */
    @Bean
    @Profile("anthropic")
    public ChatClient chatClientForAnthropic(@Qualifier("anthropicChatModel") ChatModel chatModel) {
        log.info("初始化 ChatClient，使用提供商: Anthropic Claude");
        return ChatClient.builder(chatModel)
            .defaultSystem("你是一个专业的 AI 助手，请用中文回答问题。")
            .build();
    }
}
