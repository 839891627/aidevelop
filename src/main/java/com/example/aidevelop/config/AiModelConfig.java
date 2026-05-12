package com.example.aidevelop.config;

import com.example.aidevelop.service.function.AiToolProvider;
import com.example.aidevelop.service.prompt.PromptRegistryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * AI 模型核心配置：组装基础 ChatClient（LLM + 系统提示词 + Function Calling）。
 * RAG Advisor 由路由层按请求动态注入。
 */
@Slf4j
@Configuration
public class AiModelConfig {

    private final PromptRegistryService promptRegistryService;
    private final ToolsProperties toolsProperties;
    private final Map<String, AiToolProvider> toolBeans;

    public AiModelConfig(PromptRegistryService promptRegistryService,
                         ToolsProperties toolsProperties,
                         Map<String, AiToolProvider> toolBeans) {
        this.promptRegistryService = promptRegistryService;
        this.toolsProperties = toolsProperties;
        this.toolBeans = toolBeans;
    }

    /**
     * 构建 ChatClient Bean（仅 openai profile 生效）
     * 整合：系统提示词 + Function Calling（基于 @Tool 注册）
     */
    @Bean
    @Primary
    @Profile("openai")
    public ChatClient chatClientForOpenAI(@Qualifier("openAiChatModel") ChatModel chatModel) {
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

        List<Advisor> advisors = new ArrayList<>();
        // 输出 Prompt/Response 日志，便于走读 LLM 交互链路
        advisors.add(new SimpleLoggerAdvisor());

        builder.defaultAdvisors(advisors);

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