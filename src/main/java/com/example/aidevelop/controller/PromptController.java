package com.example.aidevelop.controller;

import com.example.aidevelop.model.entity.PromptTemplateEntity;
import com.example.aidevelop.service.prompt.PromptRegistryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 提示词管理 API
 */
@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
@Tag(name = "提示词管理", description = "提示词查看和管理相关 API")
public class PromptController {

    private final PromptRegistryService promptRegistryService;

    /**
     * 获取当前 System 提示词
     */
    @GetMapping("/system")
    @Operation(summary = "获取 System 提示词", description = "获取当前正在使用的系统提示词")
    public Map<String, Object> getSystemPrompt() {
        String prompt = promptRegistryService.getSystemPrompt();
        Map<String, Object> result = new HashMap<>();
        result.put("type", "system");
        result.put("content", prompt);
        result.put("length", prompt.length());
        promptRegistryService.getActivePromptTemplate(PromptRegistryService.SYSTEM_PROMPT_KEY).ifPresent(template -> {
            result.put("source", "registry");
            result.put("version", template.getVersion());
            result.put("env", template.getEnv());
        });
        return result;
    }

    /**
     * 获取 RAG QA 提示词
     */
    @GetMapping("/rag/qa")
    @Operation(summary = "获取 RAG QA 提示词", description = "获取 RAG 问答提示词")
    public Map<String, Object> getRagQaPrompt() {
        String prompt = promptRegistryService.getRagQaPrompt();
        Map<String, Object> result = new HashMap<>();
        result.put("type", "rag-qa");
        result.put("content", prompt);
        result.put("length", prompt.length());
        promptRegistryService.getActivePromptTemplate(PromptRegistryService.RAG_QA_PROMPT_KEY).ifPresent(template -> {
            result.put("source", "registry");
            result.put("version", template.getVersion());
            result.put("env", template.getEnv());
        });
        return result;
    }

    /**
     * 获取 Function Calling 提示词
     */
    @GetMapping("/function/calling")
    @Operation(summary = "获取 Function Calling 提示词", description = "获取函数调用提示词")
    public Map<String, Object> getFunctionCallingPrompt() {
        String prompt = promptRegistryService.getFunctionCallingPrompt();
        Map<String, Object> result = new HashMap<>();
        result.put("type", "function-calling");
        result.put("content", prompt);
        result.put("length", prompt.length());
        promptRegistryService.getActivePromptTemplate(PromptRegistryService.FUNCTION_CALLING_PROMPT_KEY).ifPresent(template -> {
            result.put("source", "registry");
            result.put("version", template.getVersion());
            result.put("env", template.getEnv());
        });
        return result;
    }

    /**
     * 获取所有提示词状态
     */
    @GetMapping("/status")
    @Operation(summary = "获取提示词状态", description = "获取所有提示词的当前状态")
    public Map<String, Object> getPromptStatus() {
        Map<String, Object> status = new HashMap<>();

        // System 提示词
        String systemPrompt = promptRegistryService.getSystemPrompt();
        status.put("system", Map.of(
            "enabled", true,
            "length", systemPrompt.length(),
            "preview", systemPrompt.substring(0, Math.min(200, systemPrompt.length())) + "..."
        ));

        // RAG QA 提示词
        try {
            String ragPrompt = promptRegistryService.getRagQaPrompt();
            status.put("rag-qa", Map.of(
                "enabled", true,
                "length", ragPrompt.length(),
                "preview", ragPrompt.substring(0, Math.min(200, ragPrompt.length())) + "..."
            ));
        } catch (Exception e) {
            status.put("rag-qa", Map.of("enabled", false, "error", e.getMessage()));
        }

        // Function Calling 提示词
        try {
            String funcPrompt = promptRegistryService.getFunctionCallingPrompt();
            status.put("function-calling", Map.of(
                "enabled", true,
                "length", funcPrompt.length(),
                "preview", funcPrompt.substring(0, Math.min(200, funcPrompt.length())) + "..."
            ));
        } catch (Exception e) {
            status.put("function-calling", Map.of("enabled", false, "error", e.getMessage()));
        }

        return status;
    }

    @PostMapping("/registry/drafts")
    @Operation(summary = "创建 Prompt 草稿版本", description = "在 Prompt Registry 中创建草稿版本")
    public Map<String, Object> createDraft(@Valid @RequestBody CreateDraftRequest request) {
        PromptTemplateEntity draft = promptRegistryService.createDraft(
                request.getPromptKey(),
                request.getEnv(),
                request.getContent(),
                request.getVariablesJson(),
                request.getModelScope(),
                request.getOperator()
        );
        return Map.of(
                "message", "草稿创建成功",
                "promptKey", draft.getPromptKey(),
                "version", draft.getVersion(),
                "status", draft.getStatus(),
                "env", draft.getEnv()
        );
    }

    @PostMapping("/registry/publish")
    @Operation(summary = "发布 Prompt 版本", description = "将指定版本发布为 ACTIVE")
    public Map<String, Object> publish(@Valid @RequestBody ActivateVersionRequest request) {
        PromptTemplateEntity active = promptRegistryService.publishVersion(
                request.getPromptKey(),
                request.getEnv(),
                request.getVersion(),
                request.getOperator(),
                request.getRemark()
        );
        return Map.of(
                "message", "发布成功",
                "promptKey", active.getPromptKey(),
                "version", active.getVersion(),
                "status", active.getStatus(),
                "env", active.getEnv()
        );
    }

    @PostMapping("/registry/rollback")
    @Operation(summary = "回滚 Prompt 版本", description = "将指定版本重新激活为 ACTIVE")
    public Map<String, Object> rollback(@Valid @RequestBody ActivateVersionRequest request) {
        PromptTemplateEntity active = promptRegistryService.rollbackToVersion(
                request.getPromptKey(),
                request.getEnv(),
                request.getVersion(),
                request.getOperator(),
                request.getRemark()
        );
        return Map.of(
                "message", "回滚成功",
                "promptKey", active.getPromptKey(),
                "version", active.getVersion(),
                "status", active.getStatus(),
                "env", active.getEnv()
        );
    }

    @GetMapping("/registry/active")
    @Operation(summary = "查询当前生效 Prompt", description = "根据 promptKey 和 env 查询当前 ACTIVE 版本")
    public Map<String, Object> getActivePrompt(
            @RequestParam String promptKey,
            @RequestParam(required = false) String env
    ) {
        Optional<PromptTemplateEntity> active = promptRegistryService.getActivePrompt(promptKey, env);
        if (active.isEmpty()) {
            return Map.of(
                    "promptKey", promptKey,
                    "env", env == null ? "dev" : env,
                    "active", false
            );
        }

        PromptTemplateEntity template = active.get();
        return Map.of(
                "promptKey", template.getPromptKey(),
                "env", template.getEnv(),
                "active", true,
                "version", template.getVersion(),
                "status", template.getStatus(),
                "content", template.getContent(),
                "length", template.getContent().length()
        );
    }

    @GetMapping("/registry/versions")
    @Operation(summary = "查询 Prompt 历史版本", description = "按 promptKey 和 env 查询版本列表")
    public Map<String, Object> listVersions(
            @RequestParam String promptKey,
            @RequestParam(required = false) String env
    ) {
        List<PromptTemplateEntity> versions = promptRegistryService.listPromptVersions(promptKey, env);
        List<Map<String, Object>> items = new ArrayList<>();
        for (PromptTemplateEntity item : versions) {
            items.add(Map.of(
                    "version", item.getVersion(),
                    "status", item.getStatus(),
                    "env", item.getEnv(),
                    "createdBy", item.getCreatedBy(),
                    "updatedAt", item.getUpdatedAt(),
                    "length", item.getContent() == null ? 0 : item.getContent().length()
            ));
        }
        return Map.of(
                "promptKey", promptKey,
                "env", env == null ? "dev" : env,
                "total", items.size(),
                "items", items
        );
    }

    @Data
    public static class CreateDraftRequest {
        @NotBlank
        private String promptKey;
        private String env;
        @NotBlank
        private String content;
        private String variablesJson;
        private String modelScope;
        private String operator;
    }

    @Data
    public static class ActivateVersionRequest {
        @NotBlank
        private String promptKey;
        private String env;
        @NotNull
        private Integer version;
        private String operator;
        private String remark;
    }
}
