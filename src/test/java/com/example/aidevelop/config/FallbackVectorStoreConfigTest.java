package com.example.aidevelop.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackVectorStoreConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(FallbackVectorStoreConfig.class);

    @Test
    void shouldProvideEmptyVectorStoreWhenNoExternalStoreIsConfigured() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(VectorStore.class);

            VectorStore vectorStore = context.getBean(VectorStore.class);
            assertThat(vectorStore.similaritySearch(SearchRequest.builder()
                .query("逾期规则")
                .topK(5)
                .similarityThreshold(0.2)
                .build())).isEmpty();
        });
    }
}
