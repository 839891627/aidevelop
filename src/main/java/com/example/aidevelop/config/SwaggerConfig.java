package com.example.aidevelop.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI + Knife4j 配置
 * 访问地址：http://localhost:8080/doc.html (Knife4j)
 *         http://localhost:8080/swagger-ui.html (标准 Swagger UI)
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("AI Chat Assistant API")
                .description("基于 Spring Boot + Spring AI 的智能对话助手系统 API 文档\n\n" +
                    "**功能说明：**\n" +
                    "- 支持多个 LLM 提供商（OpenAI、Anthropic Claude、DeepSeek 等）\n" +
                    "- 支持普通聊天和流式聊天两种模式\n" +
                    "- 支持多轮对话，自动管理对话历史\n" +
                    "- 实时流式输出，提供打字机效果\n\n" +
                    "**快速开始：**\n" +
                    "1. 调用 POST /api/chat 发送消息\n" +
                    "2. 返回响应中包含 conversationId\n" +
                    "3. 后续请求传入 conversationId 实现多轮对话")
                .version("1.0.0")
                .contact(new Contact()
                    .name("AI Chat Assistant")
                    .email("your-email@example.com")
                    .url("https://github.com/your-username/aidevelop"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("本地开发环境")
            ));
    }
}
