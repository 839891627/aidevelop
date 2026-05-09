package com.example.aidevelop.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;

/**
 * RAG 配置类：负责向量库的初始化和文档的加载
 * 支持多文档自动扫描和元数据分类
 * 支持 TXT 和 PDF 格式
 */
@Configuration
@Slf4j
public class VectorStoreConfig {

    // 向量存储文件路径（存在本地，重启后数据不丢失）
    @Value("${vector.store.path:./data/vector-store.json}")
    private String vectorStorePath;

    private final VectorIndexBuilder vectorIndexBuilder;

    public VectorStoreConfig(VectorIndexBuilder vectorIndexBuilder) {
        this.vectorIndexBuilder = vectorIndexBuilder;
    }

    /**
     * 创建 SimpleVectorStore Bean
     * 使用 @Qualifier 明确指定使用 Ollama EmbeddingModel
     */
    @Bean
    public VectorStore vectorStore(@Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel) {
        // 1. 初始化 SimpleVectorStore
        File file = Path.of(vectorStorePath).toFile();

        // 确保父目录存在
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        SimpleVectorStore vectorStore = new SimpleVectorStore(embeddingModel);

        // 2. 检查本地是否已经有向量库文件
        if (file.exists()) {
            log.info("正在从本地文件加载向量库: {}", vectorStorePath);
            vectorStore.load(file);
        } else {
            log.info("未找到本地向量库文件，正在从知识库文档重新构建...");
            // 3. 如果没有，则由索引构建器读取并处理文档
            vectorIndexBuilder.buildIndex(vectorStore);
            vectorStore.save(file);
        }

        return vectorStore;
    }
}