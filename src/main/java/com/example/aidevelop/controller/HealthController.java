package com.example.aidevelop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查控制器
 */
@RestController
@Tag(name = "系统管理", description = "系统健康检查和状态查询")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查应用服务是否正常运行")
    public Map<String, String> health() {
        return Map.of(
            "status", "UP",
            "service", "ai-chat-assistant"
        );
    }
}
