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
     * 是否启用 Prompt Registry（数据库版本化管理）。
     */
    private boolean registryEnabled = true;

    /**
     * Prompt 生效环境（dev/staging/prod）。
     */
    private String env = "dev";
}
