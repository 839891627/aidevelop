package com.example.aidevelop.config;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class VectorStoreConfig {

    private final VectorStore vectorStore;
    private final VectorIndexBuilder vectorIndexBuilder;

    public VectorStoreConfig(VectorStore vectorStore, VectorIndexBuilder vectorIndexBuilder) {
        this.vectorStore = vectorStore;
        this.vectorIndexBuilder = vectorIndexBuilder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void buildIndexIfNeeded() {
        log.info("开始异步构建向量索引...");
        try {
            vectorIndexBuilder.buildIndex(vectorStore);
            log.info("向量索引构建完成");
        } catch (Exception e) {
            log.warn("向量索引构建失败，RAG 将不可用: {}", e.getMessage(), e);
        }
    }
}