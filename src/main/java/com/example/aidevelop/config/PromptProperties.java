package com.example.aidevelop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 提示词配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.prompts")
public class PromptProperties {

    /**
     * 是否启用提示词管理
     */
    private boolean enabled = true;

    /**
     * 提示词文件路径前缀
     */
    private String basePath = "classpath:prompts/";

    /**
     * System 提示词文件名
     */
    private String systemPrompt = "system/default.txt";

    /**
     * RAG QA 提示词文件名
     */
    private String ragQaPrompt = "rag/qa.txt";

    /**
     * Function Calling 提示词文件名
     */
    private String functionCallingPrompt = "function/calling.txt";
}
