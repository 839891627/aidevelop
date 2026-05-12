package com.example.aidevelop.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;

@Configuration
@Slf4j
public class VectorStoreConfig {

    @Value("${vector.store.path:./data/vector-store.json}")
    private String vectorStorePath;

    private final VectorIndexBuilder vectorIndexBuilder;
    private SimpleVectorStore vectorStoreInstance;

    public VectorStoreConfig(VectorIndexBuilder vectorIndexBuilder) {
        this.vectorIndexBuilder = vectorIndexBuilder;
    }

    @Bean
    public VectorStore vectorStore(@Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel) {
        File file = Path.of(vectorStorePath).toFile();

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        vectorStoreInstance = SimpleVectorStore.builder(embeddingModel).build();

        // 已有向量库文件则直接加载（毫秒级），否则留给启动后异步构建
        if (file.exists()) {
            log.info("从本地文件加载向量库: {}", vectorStorePath);
            vectorStoreInstance.load(file);
        } else {
            log.info("未找到向量库文件，将在应用启动后异步构建");
        }

        return vectorStoreInstance;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void buildIndexIfNeeded() {
        File file = Path.of(vectorStorePath).toFile();
        if (file.exists()) {
            return;
        }

        log.info("开始异步构建向量索引...");
        try {
            vectorIndexBuilder.buildIndex(vectorStoreInstance);
            vectorStoreInstance.save(file);
            log.info("向量索引构建完成，已持久化到: {}", vectorStorePath);
        } catch (Exception e) {
            log.warn("向量索引构建失败，RAG 将不可用: {}", e.getMessage(), e);
        }
    }
}