package com.example.aidevelop.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiHttpClientConfigTest {

    @Test
    void shouldApplyConfiguredTimeoutsToRequestFactory() {
        AiHttpProperties properties = new AiHttpProperties();
        properties.setConnectTimeoutMs(1234);
        properties.setReadTimeoutMs(5678);

        OpenAiHttpClientConfig config = new OpenAiHttpClientConfig(properties);

        SimpleClientHttpRequestFactory requestFactory = config.createRequestFactory();

        assertThat(ReflectionTestUtils.getField(requestFactory, "connectTimeout")).isEqualTo(1234);
        assertThat(ReflectionTestUtils.getField(requestFactory, "readTimeout")).isEqualTo(5678);
    }
}
