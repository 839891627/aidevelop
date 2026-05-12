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
    public static final String RAG_QA_PROMPT_KEY = "rag.qa";
    public static final String FUNCTION_CALLING_PROMPT_KEY = "function.calling";

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

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

    private String normalizeEnv(String env) {
        return StringUtils.hasText(env) ? env.trim() : "dev";
    }
}
