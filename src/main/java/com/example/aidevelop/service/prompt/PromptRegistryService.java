package com.example.aidevelop.service.prompt;

import com.example.aidevelop.config.PromptProperties;
import com.example.aidevelop.model.entity.PromptPublishLogEntity;
import com.example.aidevelop.model.entity.PromptTemplateEntity;
import com.example.aidevelop.repository.PromptPublishLogRepository;
import com.example.aidevelop.repository.PromptTemplateRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Prompt 注册中心服务：版本管理、发布、回滚、提示词解析。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptRegistryService {

    public static final String SYSTEM_PROMPT_KEY = "system.default";
    public static final String GENERAL_CHAT_PROMPT_KEY = "chat.general";
    public static final String FINANCIAL_RAG_PROMPT_KEY = "chat.financial.rag";
    public static final String RAG_QA_PROMPT_KEY = "rag.qa";
    public static final String FUNCTION_CALLING_PROMPT_KEY = "function.calling";

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String DEFAULT_GENERAL_CHAT_PROMPT = """
        你是一个通用 AI 助手。

        你的职责：
        1. 回答常规知识、技术解释、写作润色、方案设计和代码理解类问题。
        2. 不要把所有问题都限定到金融助贷领域。
        3. 如果用户的问题需要具体业务系统数据、金融知识库证据或工具执行，请提示用户切换到“金融 RAG”或“Agent 任务”。

        回答要求：
        - 直接回答用户问题。
        - 不编造事实。
        - 信息不足时说明假设或向用户追问。
        """;
    private static final String DEFAULT_FINANCIAL_RAG_PROMPT = """
        你是一个金融助贷知识库问答助手。

        你的职责：
        1. 回答借款规则、还款规则、风控政策、产品流程、额度、利率、期限等金融助贷知识库问题。
        2. 优先依据检索到的知识库资料回答。
        3. 如果检索资料不足以支持结论，必须明确说明“当前知识库证据不足”。
        4. 不查询或编造具体用户业务数据；涉及用户编号、借款记录、还款记录、风险评估时，提示用户切换到“Agent 任务”。

        回答要求：
        - 先给结论，再给依据。
        - 区分知识库事实和推断。
        - 不编造规则、阈值、流程或数值。
        """;

    private final PromptProperties promptProperties;
    private final PromptTemplateRepository promptTemplateRepository;
    private final PromptPublishLogRepository promptPublishLogRepository;

    public Optional<PromptTemplateEntity> getActivePrompt(String promptKey, String env) {
        return promptTemplateRepository.findFirstByPromptKeyAndEnvAndStatusOrderByVersionDesc(
                promptKey, normalizeEnv(env), STATUS_ACTIVE);
    }

    public List<PromptTemplateEntity> listPromptVersions(String promptKey, String env) {
        return promptTemplateRepository.findByPromptKeyAndEnvOrderByVersionDesc(
                promptKey, normalizeEnv(env));
    }

    @Transactional
    public PromptTemplateEntity createDraft(String promptKey, String env, String content,
                                            String variablesJson, String modelScope, String createdBy) {
        if (!StringUtils.hasText(promptKey)) {
            throw new IllegalArgumentException("promptKey 不能为空");
        }
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("content 不能为空");
        }

        String normalizedEnv = normalizeEnv(env);
        int nextVersion = promptTemplateRepository.findFirstByPromptKeyAndEnvOrderByVersionDesc(promptKey, normalizedEnv)
                .map(item -> item.getVersion() + 1)
                .orElse(1);

        PromptTemplateEntity draft = new PromptTemplateEntity();
        draft.setPromptKey(promptKey.trim());
        draft.setVersion(nextVersion);
        draft.setStatus(STATUS_DRAFT);
        draft.setContent(content);
        draft.setVariablesJson(variablesJson);
        draft.setModelScope(modelScope);
        draft.setEnv(normalizedEnv);
        draft.setCreatedBy(StringUtils.hasText(createdBy) ? createdBy.trim() : "system");
        return promptTemplateRepository.save(draft);
    }

    @Transactional
    public PromptTemplateEntity publishVersion(String promptKey, String env, Integer version,
                                               String operator, String remark) {
        return activateVersion(promptKey, env, version, operator, remark, "PUBLISH");
    }

    @Transactional
    public PromptTemplateEntity rollbackToVersion(String promptKey, String env, Integer version,
                                                  String operator, String remark) {
        return activateVersion(promptKey, env, version, operator, remark, "ROLLBACK");
    }

    private PromptTemplateEntity activateVersion(String promptKey, String env, Integer version,
                                                 String operator, String remark, String action) {
        String normalizedEnv = normalizeEnv(env);
        PromptTemplateEntity target = promptTemplateRepository
                .findByPromptKeyAndEnvAndVersion(promptKey, normalizedEnv, version)
                .orElseThrow(() -> new IllegalArgumentException("目标版本不存在"));

        Optional<PromptTemplateEntity> activeOpt = getActivePrompt(promptKey, normalizedEnv);
        Integer fromVersion = activeOpt.map(PromptTemplateEntity::getVersion).orElse(null);

        activeOpt.ifPresent(active -> {
            active.setStatus(STATUS_ARCHIVED);
            promptTemplateRepository.save(active);
        });

        target.setStatus(STATUS_ACTIVE);
        PromptTemplateEntity saved = promptTemplateRepository.save(target);
        writePublishLog(promptKey, normalizedEnv, action, fromVersion, version, operator, remark);
        return saved;
    }

    private void writePublishLog(String promptKey, String env, String action,
                                 Integer fromVersion, Integer toVersion,
                                 String operator, String remark) {
        PromptPublishLogEntity log = new PromptPublishLogEntity();
        log.setPromptKey(promptKey);
        log.setEnv(env);
        log.setAction(action);
        log.setFromVersion(fromVersion);
        log.setToVersion(toVersion);
        log.setOperator(StringUtils.hasText(operator) ? operator.trim() : "system");
        log.setRemark(remark);
        promptPublishLogRepository.save(log);
    }

    public String getSystemPrompt() {
        return resolvePrompt(SYSTEM_PROMPT_KEY);
    }

    public String getGeneralChatPrompt() {
        // 常规聊天的兜底 prompt 很重要：即使数据库还没初始化新 key，也不能退回金融助贷提示词。
        return resolvePromptOrDefault(GENERAL_CHAT_PROMPT_KEY, DEFAULT_GENERAL_CHAT_PROMPT);
    }

    public String getFinancialRagPrompt() {
        // 金融 RAG 独立 prompt，负责要求模型基于知识库证据回答，不查询具体用户业务数据。
        return resolvePromptOrDefault(FINANCIAL_RAG_PROMPT_KEY, DEFAULT_FINANCIAL_RAG_PROMPT);
    }

    public String getRagQaPrompt() {
        return resolvePrompt(RAG_QA_PROMPT_KEY);
    }

    public String getFunctionCallingPrompt() {
        return resolvePrompt(FUNCTION_CALLING_PROMPT_KEY);
    }

    public Optional<PromptTemplateEntity> getActivePromptTemplate(String promptKey) {
        if (!promptProperties.isRegistryEnabled()) {
            return Optional.empty();
        }
        return getActivePrompt(promptKey, promptProperties.getEnv());
    }

    private String resolvePrompt(String promptKey) {
        if (!promptProperties.isRegistryEnabled()) {
            throw new IllegalStateException("Prompt Registry 已被禁用，纯 DB 方案无法读取提示词");
        }
        if (!StringUtils.hasText(promptKey)) {
            throw new IllegalArgumentException("promptKey 不能为空");
        }
        Optional<PromptTemplateEntity> activeTemplate = getActivePrompt(
                promptKey, promptProperties.getEnv());
        if (activeTemplate.isEmpty() || !StringUtils.hasText(activeTemplate.get().getContent())) {
            throw new IllegalStateException(
                    "未找到生效 Prompt: key=%s, env=%s".formatted(promptKey, promptProperties.getEnv()));
        }
        PromptTemplateEntity template = activeTemplate.get();
        log.info("命中 Prompt Registry: key={}, env={}, version={}",
                promptKey, template.getEnv(), template.getVersion());
        return template.getContent();
    }

    private String resolvePromptOrDefault(String promptKey, String defaultContent) {
        try {
            return resolvePrompt(promptKey);
        } catch (IllegalStateException ex) {
            // 仅新模式 prompt 允许兜底，避免本地库未执行 sql/prompt_registry.sql 时聊天页不可用。
            log.warn("未命中 Prompt Registry，使用内置默认 Prompt: key={}, env={}",
                promptKey, promptProperties.getEnv());
            return defaultContent;
        }
    }

    private String normalizeEnv(String env) {
        return StringUtils.hasText(env) ? env.trim() : "dev";
    }
}
