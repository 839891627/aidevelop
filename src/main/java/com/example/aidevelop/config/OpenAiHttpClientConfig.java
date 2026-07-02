package com.example.aidevelop.config;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
@RequiredArgsConstructor
public class OpenAiHttpClientConfig {

    private final AiHttpProperties aiHttpProperties;

    @Bean
    public RestClientCustomizer openAiRestClientTimeoutCustomizer() {
        return restClientBuilder -> restClientBuilder.requestFactory(createRequestFactory());
    }

    SimpleClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(Math.max(1, aiHttpProperties.getConnectTimeoutMs())));
        requestFactory.setReadTimeout(Duration.ofMillis(Math.max(1, aiHttpProperties.getReadTimeoutMs())));
        return requestFactory;
    }
}
