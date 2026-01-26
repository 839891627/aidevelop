package com.example.aidevelop.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG 配置类：负责向量库的初始化和文档的加载
 * 支持多文档自动扫描和元数据分类
 */
@Configuration
public class VectorStoreConfig {

    // 知识库文件路径（支持通配符，扫描 knowledge 文件夹下所有 .txt 文件）
    @Value("classpath:knowledge/*.txt")
    private Resource[] knowledgeResources;

    // 向量存储文件路径（存在本地，重启后数据不丢失）
    @Value("${vector.store.path:./data/vector-store.json}")
    private String vectorStorePath;

    /**
     * 创建 SimpleVectorStore Bean
     * 使用 @Qualifier 明确指定使用智谱 AI 的 EmbeddingModel
     */
    @Bean
    public VectorStore vectorStore(@Qualifier("zhiPuAiEmbeddingModel") EmbeddingModel embeddingModel) {
        // 1. 初始化 SimpleVectorStore
        File file = Path.of(vectorStorePath).toFile();

        // 确保父目录存在
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        SimpleVectorStore vectorStore = new SimpleVectorStore(embeddingModel);

        // 2. 检查本地是否已经有向量库文件
        if (file.exists()) {
            System.out.println("正在从本地文件加载向量库: " + vectorStorePath);
            vectorStore.load(file);
        } else {
            System.out.println("未找到本地向量库文件，正在从知识库文档重新构建...");
            // 3. 如果没有，则读取并处理文档
            loadDocuments(vectorStore, embeddingModel);
            vectorStore.save(file);
        }

        return vectorStore;
    }

    /**
     * 读取文档、切分、添加元数据、向量化并存入向量库
     */
    private void loadDocuments(SimpleVectorStore vectorStore, EmbeddingModel embeddingModel) {
        try {
            List<Document> allDocuments = new ArrayList<>();

            // 1. 遍历所有知识库文件
            for (Resource resource : knowledgeResources) {
                String filename = resource.getFilename();
                System.out.println("正在处理文档: " + filename);

                // 根据文件名确定文档类型（元数据）
                String documentType = determineDocumentType(filename);

                // 2. 读取文档
                TextReader textReader = new TextReader(resource);
                List<Document> documents = textReader.get();

                // 3. 为文档添加元数据
                for (Document doc : documents) {
                    Map<String, Object> metadata = doc.getMetadata();
                    metadata.put("filename", filename);
                    metadata.put("type", documentType);  // 核心元数据：类型
                    metadata.put("source", "knowledge_base");
                }

                allDocuments.addAll(documents);
            }

            // 4. 切分文档 (TokenTextSplitter)
            TokenTextSplitter textSplitter = new TokenTextSplitter();
            List<Document> splitDocuments = textSplitter.apply(allDocuments);

            System.out.println("知识库文档切分完成，共 " + splitDocuments.size() + " 个片段。");
            System.out.println("文档分类统计:");
            System.out.println("  - 业务规则: " + countByType(splitDocuments, "规则"));
            System.out.println("  - 产品手册: " + countByType(splitDocuments, "产品"));
            System.out.println("  - 风控指南: " + countByType(splitDocuments, "风控"));

            // 5. 存入向量库
            vectorStore.add(splitDocuments);

            System.out.println("文档向量化和入库完成。");
        } catch (Exception e) {
            System.err.println("加载知识库失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 根据文件名确定文档类型
     */
    private String determineDocumentType(String filename) {
        if (filename == null) return "其他";

        if (filename.contains("rules") || filename.contains("rule")) {
            return "规则";
        } else if (filename.contains("product") || filename.contains("manual")) {
            return "产品";
        } else if (filename.contains("risk") || filename.contains("control")) {
            return "风控";
        } else {
            return "其他";
        }
    }

    /**
     * 统计指定类型的文档数量
     */
    private long countByType(List<Document> documents, String type) {
        return documents.stream()
                .filter(doc -> type.equals(doc.getMetadata().get("type")))
                .count();
    }
}