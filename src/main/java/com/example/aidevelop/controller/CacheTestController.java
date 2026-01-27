package com.example.aidevelop.controller;

import com.example.aidevelop.service.cache.CachedChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 缓存测试 API
 * 用于演示和测试缓存功能
 */
@RestController
@RequestMapping("/api/cache-test")
@RequiredArgsConstructor
@Tag(name = "缓存测试", description = "缓存功能测试和演示")
public class CacheTestController {

    private final CachedChatService cachedChatService;

    /**
     * 测试问答缓存
     * 第一次调用会请求 AI，第二次会直接返回缓存
     */
    @PostMapping("/ask")
    @Operation(summary = "测试问答缓存", description = "相同的问题会返回缓存的答案")
    public Map<String, Object> ask(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String systemPrompt = request.get("systemPrompt");

        long startTime = System.currentTimeMillis();
        String answer;

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            answer = cachedChatService.ask(systemPrompt, question);
        } else {
            answer = cachedChatService.ask(question);
        }
        long duration = System.currentTimeMillis() - startTime;

        return Map.of(
            "question", question,
            "answer", answer,
            "duration", duration + "ms",
            "note", "第二次调用相同问题时会返回缓存，速度更快"
        );
    }

    /**
     * 清除指定问题的缓存
     */
    @DeleteMapping("/ask")
    @Operation(summary = "清除问题缓存", description = "清除指定问题的缓存，下次调用会重新请求 AI")
    public Map<String, Object> evictQuestion(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        cachedChatService.evictQuestion(question);

        return Map.of(
            "success", true,
            "message", "缓存已清除: " + question
        );
    }

    /**
     * 清除所有问答缓存
     */
    @DeleteMapping("/ask/all")
    @Operation(summary = "清除所有问答缓存", description = "清除所有问答缓存")
    public Map<String, Object> evictAllQuestions() {
        cachedChatService.evictAllQuestions();

        return Map.of(
            "success", true,
            "message", "所有问答缓存已清除"
        );
    }

    /**
     * 缓存对比测试
     * 演示有缓存和无缓存的性能差异
     */
    @GetMapping("/compare")
    @Operation(summary = "缓存对比测试", description = "对比有缓存和无缓存的性能差异")
    public Map<String, Object> compareCache(@RequestParam String question) {
        long start1, start2;
        String answer1, answer2;
        long duration1, duration2;

        // 第一次调用（无缓存）
        start1 = System.currentTimeMillis();
        answer1 = cachedChatService.ask(question);
        duration1 = System.currentTimeMillis() - start1;

        // 第二次调用（有缓存）
        start2 = System.currentTimeMillis();
        answer2 = cachedChatService.ask(question);
        duration2 = System.currentTimeMillis() - start2;

        return Map.of(
            "question", question,
            "firstCall", Map.of(
                "duration", duration1 + "ms",
                "fromCache", false
            ),
            "secondCall", Map.of(
                "duration", duration2 + "ms",
                "fromCache", true,
                "speedup", String.format("%.1fx", (double) duration1 / duration2)
            ),
            "answer", answer1
        );
    }
}
