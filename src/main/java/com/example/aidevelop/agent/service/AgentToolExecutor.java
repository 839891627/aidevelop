package com.example.aidevelop.agent.service;

import com.example.aidevelop.agent.model.ToolCall;
import com.example.aidevelop.agent.tool.ToolRouter;
import com.example.aidevelop.config.AgentProperties;
import com.example.aidevelop.exception.AiServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentToolExecutor {

    private final ToolRouter toolRouter;
    private final AgentProperties agentProperties;
    private final ObjectMapper objectMapper;
    private final AgentRateLimiter rateLimiter;

    public AgentToolExecutionResult executeWithRetry(ToolCall toolCall) {
        int totalAttempts = Math.max(1, agentProperties.getToolMaxRetries() + 1);
        long startedAt = System.currentTimeMillis();
        String lastError = null;
        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            // L3 限流：按 toolName 令牌桶 + 并发数，超时返回失败 result 走降级（不抛异常、不重试）。
            try (AgentRateLimiter.Permit permit = rateLimiter.acquireTool(toolCall.toolName()).orElse(null)) {
                if (permit == null) {
                    return new AgentToolExecutionResult(false, "RATE_LIMITED",
                        "工具限流: " + toolCall.toolName(), 0, System.currentTimeMillis() - startedAt);
                }
                Object result = executeWithTimeout(toolCall);
                return new AgentToolExecutionResult(
                    true,
                    toJson(result),
                    null,
                    attempt,
                    System.currentTimeMillis() - startedAt
                );
            } catch (Exception ex) {
                lastError = ex.getMessage() == null ? "工具执行失败" : ex.getMessage();
                if (attempt < totalAttempts && agentProperties.getRetryBackoffMs() > 0) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(agentProperties.getRetryBackoffMs());
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.warn("Agent tool 执行失败(已重试): toolName={}, attempts={}, error={}",
            toolCall.toolName(), totalAttempts, lastError);
        return new AgentToolExecutionResult(false, "FAILED", lastError, totalAttempts, System.currentTimeMillis() - startedAt);
    }

    private Object executeWithTimeout(ToolCall toolCall) {
        AgentTraceContext.Context context = AgentTraceContext.get();
        try {
            return CompletableFuture
                .supplyAsync(() -> {
                    if (context != null) {
                        AgentTraceContext.set(context);
                    }
                    try {
                        return toolRouter.execute(toolCall.toolName(), toolCall.args());
                    } finally {
                        AgentTraceContext.clear();
                    }
                })
                .orTimeout(resolveTimeoutMs(toolCall), TimeUnit.MILLISECONDS)
                .join();
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof TimeoutException) {
                throw new AiServiceException("工具调用超时: " + toolCall.toolName(), cause);
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new AiServiceException("工具调用失败: " + toolCall.toolName(), cause);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }

    private int resolveTimeoutMs(ToolCall toolCall) {
        if (toolCall != null
            && agentProperties.getToolTimeoutMs() != null
            && agentProperties.getToolTimeoutMs().containsKey(toolCall.toolName())) {
            Integer timeout = agentProperties.getToolTimeoutMs().get(toolCall.toolName());
            if (timeout != null && timeout > 0) {
                return timeout;
            }
        }
        return agentProperties.getTimeoutMs();
    }
}
