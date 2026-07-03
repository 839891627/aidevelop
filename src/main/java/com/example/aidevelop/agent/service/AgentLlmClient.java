package com.example.aidevelop.agent.service;

import com.example.aidevelop.config.AgentProperties;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Agent LLM 阶段调用入口，集中处理预算、trace 上下文和超时。
 */
@Component
@RequiredArgsConstructor
public class AgentLlmClient {

    @Resource(name = "chatClientForOpenAI")
    private ChatClient chatClient;

    private final AgentProperties agentProperties;

    public String call(String phase, String prompt) {
        return call(phase, prompt, null);
    }

    public String call(String phase, String prompt, Integer maxTokens) {
        AgentTraceContext.Context context = AgentTraceContext.withPhase(phase);
        if (context != null && context.budgetTracker() != null) {
            context.budgetTracker().recordLlmCall(phase);
        }

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            if (context != null) {
                AgentTraceContext.set(context);
            }
            try {
                ChatClient.ChatClientRequestSpec spec = chatClient.prompt().user(prompt);
                if (maxTokens != null && maxTokens > 0) {
                    spec = spec.options(OpenAiChatOptions.builder().maxTokens(maxTokens).build());
                }
                return spec.call().content();
            } finally {
                AgentTraceContext.clear();
            }
        });

        try {
            return future.orTimeout(Math.max(1, agentProperties.getLlmTimeoutMs()), TimeUnit.MILLISECONDS).join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof TimeoutException) {
                throw new AgentLlmTimeoutException("LLM 调用超时: " + phase, cause);
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("LLM 调用失败: " + phase, cause);
        }
    }
}
