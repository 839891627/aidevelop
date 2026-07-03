package com.example.aidevelop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai.http")
public class AiHttpProperties {

    private int connectTimeoutMs = 10000;
    private int readTimeoutMs = 60000;
}
