package com.example.aidevelop.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
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
 * 支持 TXT 和 PDF 格式
 */
@Configuration
public class VectorStoreConfig {

    // 知识库文件路径（支持通配符，扫描 knowledge 文件夹下所有 .txt 和 .pdf 文件）
    @Value("classpath:knowledge/*.txt")
    private Resource[] textResources;

    @Value("classpath:knowledge/*.pdf")
    private Resource[] pdfResources;

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
     * 支持 TXT 和 PDF 格式
     */
    private void loadDocuments(SimpleVectorStore vectorStore, EmbeddingModel embeddingModel) {
        try {
            List<Document> allDocuments = new ArrayList<>();

            // 1. 加载 TXT 文档
            for (Resource resource : textResources) {
                String filename = resource.getFilename();
                System.out.println("正在处理 TXT 文档: " + filename);

                TextReader textReader = new TextReader(resource);
                List<Document> documents = textReader.get();

                // 添加元数据
                for (Document doc : documents) {
                    Map<String, Object> metadata = doc.getMetadata();
                    metadata.put("filename", filename);
                    metadata.put("type", determineDocumentType(filename));
                    metadata.put("source", "knowledge_base");
                    metadata.put("fileType", "txt");
                }

                allDocuments.addAll(documents);
            }

            // 2. 加载 PDF 文档
            for (Resource resource : pdfResources) {
                String filename = resource.getFilename();
                System.out.println("正在处理 PDF 文档: " + filename);

                try {
                    // 使用 PagePdfDocumentReader 读取 PDF（按页分割）
                    PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
                    List<Document> documents = pdfReader.get();

                    // 添加元数据
                    for (Document doc : documents) {
                        Map<String, Object> metadata = doc.getMetadata();
                        metadata.put("filename", filename);
                        metadata.put("type", determineDocumentType(filename));
                        metadata.put("source", "knowledge_base");
                        metadata.put("fileType", "pdf");

                        // PDF 特有的元数据（如果有的话）
                        Object pageNum = metadata.get("page_number");
                        if (pageNum != null) {
                            System.out.println("  - 第 " + pageNum + " 页");
                        }
                    }

                    allDocuments.addAll(documents);
                    System.out.println("  - PDF 解析成功，共 " + documents.size() + " 页");
                } catch (Exception e) {
                    System.err.println("  - PDF 解析失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // 3. 切分文档 (TokenTextSplitter)
            TokenTextSplitter textSplitter = new TokenTextSplitter();
            List<Document> splitDocuments = textSplitter.apply(allDocuments);

            System.out.println("知识库文档切分完成，共 " + splitDocuments.size() + " 个片段。");
            System.out.println("文档分类统计:");
            System.out.println("  - 业务规则: " + countByType(splitDocuments, "规则"));
            System.out.println("  - 产品手册: " + countByType(splitDocuments, "产品"));
            System.out.println("  - 风控指南: " + countByType(splitDocuments, "风控"));
            System.out.println("  - 合同模板: " + countByType(splitDocuments, "合同"));
            System.out.println("  - 文件类型统计: TXT=" + countByFileType(splitDocuments, "txt") + ", PDF=" + countByFileType(splitDocuments, "pdf"));

            // 4. 存入向量库
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
        } else if (filename.contains("contract") || filename.contains("template")) {
            return "合同";
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

    /**
     * 统计指定文件类型的文档数量
     */
    private long countByFileType(List<Document> documents, String fileType) {
        return documents.stream()
                .filter(doc -> fileType.equals(doc.getMetadata().get("fileType")))
                .count();
    }
}