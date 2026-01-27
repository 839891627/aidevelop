package com.example.aidevelop.service.prompt;

import com.example.aidevelop.config.PromptProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 提示词管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptService {

    private final ResourceLoader resourceLoader;
    private final PromptProperties promptProperties;

    /**
     * 获取 System 提示词
     */
    public String getSystemPrompt() {
        return loadPrompt(promptProperties.getSystemPrompt());
    }

    /**
     * 获取指定文件的 System 提示词
     */
    public String getSystemPrompt(String filename) {
        return loadPrompt("system/" + filename);
    }

    /**
     * 获取 RAG QA 提示词
     */
    public String getRagQaPrompt() {
        return loadPrompt(promptProperties.getRagQaPrompt());
    }

    /**
     * 获取 Function Calling 提示词
     */
    public String getFunctionCallingPrompt() {
        return loadPrompt(promptProperties.getFunctionCallingPrompt());
    }

    /**
     * 加载提示词文件
     */
    public String loadPrompt(String location) {
        String fullPath = promptProperties.getBasePath() + location;
        log.debug("加载提示词文件: {}", fullPath);

        try {
            Resource resource = resourceLoader.getResource(fullPath);
            if (!resource.exists()) {
                log.warn("提示词文件不存在: {}，使用默认提示词", fullPath);
                return getDefaultPrompt();
            }

            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            log.info("成功加载提示词文件: {} (长度: {} 字符)", fullPath, content.length());
            return content;

        } catch (IOException e) {
            log.error("加载提示词文件失败: {}", fullPath, e);
            return getDefaultPrompt();
        }
    }

    /**
     * 获取默认提示词（降级方案）
     */
    private String getDefaultPrompt() {
        return """
            你是一个专业的金融系统AI助手。

            你可以帮助用户：
            1. 查询借款和还款记录
            2. 评估客户风险
            3. 回答业务规则问题

            请基于提供的上下文信息回答用户的问题，不要编造内容。
            """;
    }

    /**
     * 重新加载提示词（用于运行时更新）
     */
    public String reloadSystemPrompt() {
        log.info("重新加载 System 提示词");
        return getSystemPrompt();
    }
}
