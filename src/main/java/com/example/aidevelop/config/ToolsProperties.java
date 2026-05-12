package com.example.aidevelop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具注册配置属性。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.tools")
public class ToolsProperties {

    /**
     * 启用的工具 Bean 名称列表。
     * 为空时表示启用全部工具。
     */
    private List<String> enabled = new ArrayList<>();
}
