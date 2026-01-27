package com.example.aidevelop.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/**
 * 带缓存的问答服务
 * 缓存相同问题的回答，避免重复调用 AI API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CachedChatService {

    private final ChatClient chatClient;

    /**
     * 简单问答（带缓存）
     * 适用于无状态的单次问答
     *
     * @param question 用户问题
     * @return AI 回答
     */
    @Cacheable(
        value = "qaCache",
        key = "#question",
        cacheManager = "aiResponseCacheManager"
    )
    public String ask(String question) {
        log.info("AI 问答（未缓存）: question={}", question);

        ChatResponse response = chatClient.prompt()
                .user(question)
                .call()
                .chatResponse();

        return response.getResult().getOutput().getContent();
    }

    /**
     * 带系统提示的问答（带缓存）
     */
    @Cacheable(
        value = "qaCache",
        key = "'system:' + #systemPrompt + '|question:' + #question",
        cacheManager = "aiResponseCacheManager"
    )
    public String ask(String systemPrompt, String question) {
        log.info("AI 问答（带系统提示，未缓存）: question={}", question);

        ChatResponse response = chatClient.prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .chatResponse();

        return response.getResult().getOutput().getContent();
    }

    /**
     * 清除指定问题的缓存
     */
    @CacheEvict(value = "qaCache", key = "#question")
    public void evictQuestion(String question) {
        log.info("清除问答缓存: question={}", question);
    }

    /**
     * 清除所有问答缓存
     */
    @CacheEvict(value = "qaCache", allEntries = true)
    public void evictAllQuestions() {
        log.info("清除所有问答缓存");
    }
}
