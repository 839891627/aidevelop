package com.example.aidevelop.controller;

import com.example.aidevelop.service.prompt.PromptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 提示词管理 API
 */
@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
@Tag(name = "提示词管理", description = "提示词查看和管理相关 API")
public class PromptController {

    private final PromptService promptService;

    /**
     * 获取当前 System 提示词
     */
    @GetMapping("/system")
    @Operation(summary = "获取 System 提示词", description = "获取当前正在使用的系统提示词")
    public Map<String, Object> getSystemPrompt() {
        String prompt = promptService.getSystemPrompt();
        return Map.of(
            "type", "system",
            "content", prompt,
            "length", prompt.length()
        );
    }

    /**
     * 获取指定版本的 System 提示词
     */
    @GetMapping("/system/{filename}")
    @Operation(summary = "获取指定版本的 System 提示词", description = "从文件加载指定版本的系统提示词")
    public Map<String, Object> getSystemPrompt(@PathVariable String filename) {
        String prompt = promptService.getSystemPrompt(filename);
        return Map.of(
            "type", "system",
            "filename", filename,
            "content", prompt,
            "length", prompt.length()
        );
    }

    /**
     * 重新加载 System 提示词
     */
    @PostMapping("/system/reload")
    @Operation(summary = "重新加载 System 提示词", description = "从文件重新加载系统提示词（用于运行时更新）")
    public Map<String, Object> reloadSystemPrompt() {
        String oldPrompt = promptService.getSystemPrompt();
        String newPrompt = promptService.reloadSystemPrompt();

        return Map.of(
            "message", "提示词已重新加载",
            "oldLength", oldPrompt.length(),
            "newLength", newPrompt.length(),
            "changed", !oldPrompt.equals(newPrompt)
        );
    }

    /**
     * 获取 RAG QA 提示词
     */
    @GetMapping("/rag/qa")
    @Operation(summary = "获取 RAG QA 提示词", description = "获取 RAG 问答提示词")
    public Map<String, Object> getRagQaPrompt() {
        String prompt = promptService.getRagQaPrompt();
        return Map.of(
            "type", "rag-qa",
            "content", prompt,
            "length", prompt.length()
        );
    }

    /**
     * 获取 Function Calling 提示词
     */
    @GetMapping("/function/calling")
    @Operation(summary = "获取 Function Calling 提示词", description = "获取函数调用提示词")
    public Map<String, Object> getFunctionCallingPrompt() {
        String prompt = promptService.getFunctionCallingPrompt();
        return Map.of(
            "type", "function-calling",
            "content", prompt,
            "length", prompt.length()
        );
    }

    /**
     * 加载自定义提示词文件
     */
    @GetMapping("/load")
    @Operation(summary = "加载自定义提示词", description = "从指定路径加载提示词文件")
    public Map<String, Object> loadCustomPrompt(@RequestParam String location) {
        String prompt = promptService.loadPrompt(location);
        return Map.of(
            "location", location,
            "content", prompt,
            "length", prompt.length()
        );
    }

    /**
     * 获取所有提示词状态
     */
    @GetMapping("/status")
    @Operation(summary = "获取提示词状态", description = "获取所有提示词的当前状态")
    public Map<String, Object> getPromptStatus() {
        Map<String, Object> status = new HashMap<>();

        // System 提示词
        String systemPrompt = promptService.getSystemPrompt();
        status.put("system", Map.of(
            "enabled", true,
            "length", systemPrompt.length(),
            "preview", systemPrompt.substring(0, Math.min(200, systemPrompt.length())) + "..."
        ));

        // RAG QA 提示词
        try {
            String ragPrompt = promptService.getRagQaPrompt();
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
            String funcPrompt = promptService.getFunctionCallingPrompt();
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
}
