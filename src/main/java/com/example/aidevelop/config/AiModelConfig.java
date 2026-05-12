package com.example.aidevelop.config;

import com.example.aidevelop.service.function.AiToolProvider;
import com.example.aidevelop.service.prompt.PromptRegistryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * AI 模型核心配置：组装 ChatClient（LLM + 系统提示词 + Function Calling + RAG）
 */
@Slf4j
@Configuration
public class AiModelConfig {

    private final RagProperties ragProperties;
    private final PromptRegistryService promptRegistryService;
    private final ToolsProperties toolsProperties;
    private final Map<String, AiToolProvider> toolBeans;

    public AiModelConfig(RagProperties ragProperties,
                         PromptRegistryService promptRegistryService,
                         ToolsProperties toolsProperties,
                         Map<String, AiToolProvider> toolBeans) {
        this.ragProperties = ragProperties;
        this.promptRegistryService = promptRegistryService;
        this.toolsProperties = toolsProperties;
        this.toolBeans = toolBeans;
    }

    /**
     * 构建 ChatClient Bean（仅 openai profile 生效）
     * 整合：系统提示词 + Function Calling（基于 @Tool 注册） + RAG Advisor
     */
    @Bean
    @Profile("openai")
    public ChatClient chatClientForOpenAI(@Qualifier("openAiChatModel") ChatModel chatModel,
                                           @Qualifier("vectorStore") ObjectProvider<VectorStore> vectorStoreProvider) {
        log.info("初始化 ChatClient，使用提供商: OpenAI (DeepSeek)");
        List<Object> activeTools = resolveActiveTools();
        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(promptRegistryService.getSystemPrompt());
        if (!activeTools.isEmpty()) {
            builder.defaultTools(activeTools.toArray());
            log.info("注册 @Tool 工具数量: {}", activeTools.size());
        } else {
            log.warn("未启用任何 @Tool 工具，Function Calling 将不可用");
        }

        // 条件装配 RAG：启用时注入 QuestionAnswerAdvisor 实现检索增强
        if (ragProperties.isEnabled()) {
            VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
            if (vectorStore != null) {
                SearchRequest searchRequest = SearchRequest.builder()
                    .topK(ragProperties.getTopK())
                    .similarityThreshold(ragProperties.getSimilarityThreshold())
                    .build();
                builder.defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore)
                    .searchRequest(searchRequest)
                    .build());
                log.info("RAG 检索已启用");
            } else {
                log.warn("RAG 已配置启用，但未找到 VectorStore Bean，将跳过检索增强");
            }
        } else {
            log.info("RAG 检索已禁用，仅使用 DeepSeek 聊天");
        }

        return builder.build();
    }

    private List<Object> resolveActiveTools() {
        List<String> enabledList = toolsProperties.getEnabled();
        if (enabledList == null || enabledList.isEmpty()) {
            List<Object> allTools = toolBeans.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(Map.Entry::getValue)
                .map(tool -> (Object) tool)
                .toList();
            log.info("app.tools.enabled 未配置，启用全部工具: {}", toolBeans.keySet());
            return allTools;
        }

        List<Object> selected = new ArrayList<>();
        for (String beanName : enabledList) {
            AiToolProvider tool = toolBeans.get(beanName);
            if (tool == null) {
                log.warn("工具未找到，跳过注册: {}", beanName);
                continue;
            }
            selected.add(tool);
        }
        log.info("按配置启用工具: {}", enabledList);
        return selected;
    }
}