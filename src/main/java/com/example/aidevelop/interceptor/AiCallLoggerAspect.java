package com.example.aidevelop.interceptor;

import com.example.aidevelop.model.entity.AiCallLog;
import com.example.aidevelop.repository.AiCallLogRepository;
import com.example.aidevelop.service.cost.AiCostCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * AI 调用日志拦截器
 * 自动记录所有 AI 模型调用，包括 Chat 和 Embedding
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AiCallLoggerAspect {

    private final AiCallLogRepository aiCallLogRepository;
    private final AiCostCalculator costCalculator;
    @Value("${spring.ai.openai.chat.options.model}")
    private String configuredChatModel;
    @Value("${spring.ai.ollama.embedding.options.model}")
    private String configuredEmbeddingModel;

    /**
     * 拦截 ChatModel.call() 方法
     */
    @Around("execution(* org.springframework.ai.chat.model.ChatModel.call(..))")
    public Object logChatCall(ProceedingJoinPoint joinPoint) throws Throwable {
        return logAiCall(joinPoint, ModelType.CHAT);
    }

    /**
     * 拦截 EmbeddingModel.embed() 方法
     */
    @Around("execution(* org.springframework.ai.embedding.EmbeddingModel.embed(..))")
    public Object logEmbeddingCall(ProceedingJoinPoint joinPoint) throws Throwable {
        return logAiCall(joinPoint, ModelType.EMBEDDING);
    }

    /**
     * 通用的 AI 调用日志记录方法
     */
    @Transactional
    protected Object logAiCall(ProceedingJoinPoint joinPoint, ModelType modelType) throws Throwable {
        long startTime = System.currentTimeMillis();
        String sessionId = UUID.randomUUID().toString();
        String modelName = "unknown";
        String provider = "unknown";
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            // 获取模型信息
            Object target = joinPoint.getTarget();
            if (target instanceof ChatModel) {
                ChatModel chatModel = (ChatModel) target;
                modelName = getModelNameFromModel(chatModel.getClass().getSimpleName(), modelType);
                provider = getProviderFromModel(chatModel.getClass().getSimpleName());
            } else if (target instanceof EmbeddingModel) {
                EmbeddingModel embeddingModel = (EmbeddingModel) target;
                modelName = getModelNameFromModel(embeddingModel.getClass().getSimpleName(), modelType);
                provider = getProviderFromModel(embeddingModel.getClass().getSimpleName());
            }

            // 执行调用
            Object result = joinPoint.proceed();

            // 记录成功的调用
            long latency = System.currentTimeMillis() - startTime;
            saveCallLog(sessionId, modelName, provider, modelType, result, latency, status, null);

            return result;

        } catch (Exception e) {
            status = "FAILURE";
            errorMessage = e.getMessage();
            long latency = System.currentTimeMillis() - startTime;

            // 记录失败的调用
            saveCallLog(sessionId, modelName, provider, modelType, null, latency, status, errorMessage);

            throw e;
        }
    }

    /**
     * 保存调用日志
     */
    private void saveCallLog(String sessionId, String modelName, String provider,
                             ModelType modelType, Object result, long latency,
                             String status, String errorMessage) {
        try {
            AiCallLog callLog = new AiCallLog();
            callLog.setSessionId(sessionId);
            callLog.setUserId("anonymous"); // 可以从 SecurityContext 获取
            callLog.setModelName(modelName);
            callLog.setModelType(modelType.name());
            callLog.setProvider(provider);
            callLog.setLatencyMs(latency);
            callLog.setStatus(status);
            callLog.setErrorMessage(errorMessage);

            // 从响应中提取 token 信息并计算成本
            if (result != null && "SUCCESS".equals(status)) {
                extractTokensAndCost(callLog, result);
            }

            aiCallLogRepository.save(callLog);
            log.debug("AI 调用日志已保存: model={}, tokens={}, cost={}",
                callLog.getModelName(), callLog.getTotalTokens(), callLog.getCost());

        } catch (Exception e) {
            log.error("保存 AI 调用日志失败", e);
            // 不抛出异常，避免影响主流程
        }
    }

    /**
     * 从响应中提取 token 信息并计算成本
     */
    private void extractTokensAndCost(AiCallLog callLog, Object result) {
        try {
            if (result instanceof ChatResponse) {
                ChatResponse response = (ChatResponse) result;
                if (response.getMetadata() != null) {
                    // 提取 token 使用情况
                    Object usage = response.getMetadata().get("usage");
                    if (usage != null) {
                        // 尝试从 metadata 中获取 token 信息
                        // 注意：这里需要根据实际的 Spring AI 响应结构调整
                        callLog.setTotalTokens(estimateTokens(result));  // 估算
                    } else {
                        callLog.setTotalTokens(estimateTokens(result));  // 估算
                    }
                } else {
                    callLog.setTotalTokens(estimateTokens(result));  // 估算
                }

                // 计算成本
                if (costCalculator.isModelSupported(callLog.getModelName())) {
                    int promptTokens = callLog.getPromptTokens() != null ? callLog.getPromptTokens() : callLog.getTotalTokens() / 2;
                    int completionTokens = callLog.getCompletionTokens() != null ? callLog.getCompletionTokens() : callLog.getTotalTokens() / 2;
                    BigDecimal cost = costCalculator.calculateCost(callLog.getModelName(), promptTokens, completionTokens);
                    callLog.setCost(cost);
                }
            } else if (result instanceof EmbeddingResponse) {
                EmbeddingResponse response = (EmbeddingResponse) result;
                // 估算 token 数（粗略估算：字符数/2）
                int estimatedTokens = estimateTokens(result);
                callLog.setTotalTokens(estimatedTokens);
                callLog.setPromptTokens(estimatedTokens);
                callLog.setCompletionTokens(0);

                // 计算成本
                if (costCalculator.isModelSupported(callLog.getModelName())) {
                    BigDecimal cost = costCalculator.calculateCost(callLog.getModelName(), estimatedTokens, 0);
                    callLog.setCost(cost);
                }
            }
        } catch (Exception e) {
            log.warn("提取 token 信息或计算成本失败: {}", e.getMessage());
            callLog.setTotalTokens(0);
            callLog.setCost(BigDecimal.ZERO);
        }
    }

    /**
     * 估算 token 数（当无法从响应中获取时）
     */
    private int estimateTokens(Object result) {
        try {
            if (result instanceof ChatResponse) {
                ChatResponse response = (ChatResponse) result;
                // 获取所有消息并估算
                StringBuilder text = new StringBuilder();
                if (response.getResults() != null) {
                    response.getResults().forEach(r -> {
                        if (r.getOutput() != null) {
                            text.append(r.getOutput().getText());
                        }
                    });
                }
                // 粗略估算：中文字符数 + 英文单词数
                return text.length() / 2;
            } else if (result instanceof EmbeddingResponse) {
                EmbeddingResponse response = (EmbeddingResponse) result;
                // 估算输入文本的 token 数
                return response.getResults().size() * 100; // 粗略估算
            }
        } catch (Exception e) {
            log.warn("估算 token 失败: {}", e.getMessage());
        }
        return 0;
    }

    /**
     * 从模型类名推断模型名称
     */
    private String getModelNameFromModel(String className, ModelType modelType) {
        if (className.contains("OpenAi")) {
            return modelType == ModelType.EMBEDDING ? configuredEmbeddingModel : configuredChatModel;
        } else if (className.contains("Ollama")) {
            return configuredEmbeddingModel;
        }
        return "unknown";
    }

    /**
     * 从模型类名推断提供商
     */
    private String getProviderFromModel(String className) {
        if (className.contains("OpenAi")) {
            return "OPENAI";
        } else if (className.contains("Ollama")) {
            return "OLLAMA";
        }
        return "UNKNOWN";
    }

    /**
     * 模型类型枚举
     */
    private enum ModelType {
        CHAT,       // 对话模型
        EMBEDDING   // 向量化模型
    }
}
