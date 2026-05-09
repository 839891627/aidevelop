package com.example.aidevelop.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * OpenAI 兼容聊天配置校验
 * 在应用启动阶段尽早失败，避免运行时才出现难定位的调用异常。
 */
@Configuration
public class OpenAiConfigValidator {

    @Value("${spring.ai.openai.chat.enabled:true}")
    private boolean chatEnabled;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:}")
    private String baseUrl;

    @PostConstruct
    public void validate() {
        if (!chatEnabled) {
            return;
        }

        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException(
                "OpenAI chat is enabled, but spring.ai.openai.api-key is empty. " +
                    "Please set OPENAI_API_KEY.");
        }

        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException(
                "OpenAI chat is enabled, but spring.ai.openai.base-url is empty. " +
                    "Please set OPENAI_BASE_URL.");
        }
    }
}

