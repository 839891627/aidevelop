package com.example.aidevelop.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * 向量索引构建器
 * 负责知识库文档读取、切分、元数据补充和入库。
 */
@Slf4j
@Component
public class VectorIndexBuilder {

    @Value("classpath:knowledge/*.txt")
    private Resource[] textResources;

    @Value("classpath:knowledge/*.pdf")
    private Resource[] pdfResources;

    private final RagProperties ragProperties;

    public VectorIndexBuilder(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    public void buildIndex(SimpleVectorStore vectorStore) {
        try {
            List<Document> allDocuments = new ArrayList<>();
            loadTextDocuments(allDocuments);
            loadPdfDocuments(allDocuments);

            // 切分策略说明见 docs/11-embedding-and-chunking.md
            RagProperties.Chunking chunking = ragProperties.getChunking();
            TokenTextSplitter textSplitter = TokenTextSplitter.builder()
                .withChunkSize(chunking.getChunkSize())
                .withMinChunkSizeChars(chunking.getMinChunkSizeChars())
                .withMinChunkLengthToEmbed(chunking.getMinChunkLengthToEmbed())
                .withMaxNumChunks(chunking.getMaxNumChunks())
                .withKeepSeparator(chunking.isKeepSeparator())
                .build();
            List<Document> splitDocuments = textSplitter.apply(allDocuments);

            log.info("知识库文档切分完成，共 {} 个片段", splitDocuments.size());
            log.info("文档分类统计: 规则={}, 产品={}, 风控={}, 合同={}, TXT={}, PDF={}",
                countByType(splitDocuments, "规则"),
                countByType(splitDocuments, "产品"),
                countByType(splitDocuments, "风控"),
                countByType(splitDocuments, "合同"),
                countByFileType(splitDocuments, "txt"),
                countByFileType(splitDocuments, "pdf"));

            vectorStore.add(splitDocuments);
            log.info("文档向量化和入库完成");
        } catch (Exception e) {
            log.error("加载知识库失败: {}", e.getMessage(), e);
            throw new IllegalStateException("向量库初始化失败", e);
        }
    }

    private void loadTextDocuments(List<Document> allDocuments) {
        for (Resource resource : textResources) {
            String filename = resource.getFilename();
            log.info("正在处理 TXT 文档: {}", filename);

            TextReader textReader = new TextReader(resource);
            List<Document> documents = textReader.get();
            appendMetadata(documents, filename, "txt");
            allDocuments.addAll(documents);
        }
    }

    private void loadPdfDocuments(List<Document> allDocuments) {
        for (Resource resource : pdfResources) {
            String filename = resource.getFilename();
            log.info("正在处理 PDF 文档: {}", filename);
            try {
                PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
                List<Document> documents = pdfReader.get();
                appendMetadata(documents, filename, "pdf");
                allDocuments.addAll(documents);
                log.info("PDF 解析成功，共 {} 页", documents.size());
            } catch (Exception e) {
                log.error("PDF 解析失败: {}", e.getMessage(), e);
            }
        }
    }

    private void appendMetadata(List<Document> documents, String filename, String fileType) {
        for (Document doc : documents) {
            Map<String, Object> metadata = doc.getMetadata();
            metadata.put("filename", filename);
            metadata.put("type", determineDocumentType(filename));
            metadata.put("source", "knowledge_base");
            metadata.put("fileType", fileType);
        }
    }

    private String determineDocumentType(String filename) {
        if (filename == null) {
            return "其他";
        }
        if (filename.contains("rules") || filename.contains("rule")) {
            return "规则";
        } else if (filename.contains("product") || filename.contains("manual")) {
            return "产品";
        } else if (filename.contains("risk") || filename.contains("control")) {
            return "风控";
        } else if (filename.contains("contract") || filename.contains("template")) {
            return "合同";
        }
        return "其他";
    }

    private long countByType(List<Document> documents, String type) {
        return documents.stream().filter(doc -> type.equals(doc.getMetadata().get("type"))).count();
    }

    private long countByFileType(List<Document> documents, String fileType) {
        return documents.stream().filter(doc -> fileType.equals(doc.getMetadata().get("fileType"))).count();
    }
}

