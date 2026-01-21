package com.example.aidevelop.controller;

import com.example.aidevelop.model.dto.ModelInfo;
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
public class ModelController {

    @Value("${spring.profiles.active:openai}")
    private String activeProfile;

    @GetMapping("/current")
    public ModelInfo getCurrentModel() {
        return ModelInfo.builder()
            .provider(activeProfile)
            .isActive(true)
            .build();
    }

    @GetMapping
    public List<ModelInfo> listModels() {
        return List.of(
            ModelInfo.builder().provider("openai").model("gpt-4-turbo-preview").build(),
            ModelInfo.builder().provider("anthropic").model("claude-3-5-sonnet").build()
        );
    }
}
