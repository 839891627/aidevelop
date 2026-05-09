package com.example.aidevelop.controller;

import com.example.aidevelop.model.dto.ModelInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模型管理控制器
 */
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
@Tag(name = "模型管理", description = "AI 模型信息查询")
public class ModelController {

    @Value("${spring.ai.openai.chat.options.model}")
    private String chatModel;

    @Value("${spring.profiles.active:openai}")
    private String activeProfile;

    @GetMapping("/current")
    @Operation(summary = "获取当前使用的模型", description = "返回当前激活的 AI 模型提供商信息")
    public ModelInfo getCurrentModel() {
        return ModelInfo.builder()
            .provider(activeProfile)
            .isActive(true)
            .build();
    }

    @GetMapping
    @Operation(summary = "获取所有可用模型", description = "返回系统支持的所有 AI 模型列表")
    public List<ModelInfo> listModels() {
        return List.of(
            ModelInfo.builder().provider("openai").model(chatModel).build()
        );
    }
}
