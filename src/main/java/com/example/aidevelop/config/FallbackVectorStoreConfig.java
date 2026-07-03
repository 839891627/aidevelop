package com.example.aidevelop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Slf4j
public class FallbackVectorStoreConfig {

    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    @ConditionalOnProperty(prefix = "spring.ai.vectorstore.milvus", name = "enabled", havingValue = "false", matchIfMissing = true)
    public VectorStore fallbackVectorStore() {
        log.warn("未配置可用的 VectorStore，RAG 将使用空结果降级实现。需要真实检索时请启动 Milvus 并设置 MILVUS_ENABLED=true");
        return new EmptyVectorStore();
    }

    private static class EmptyVectorStore implements VectorStore {

        @Override
        public void add(List<Document> documents) {
            log.debug("fallback VectorStore 忽略文档写入，documents={}", documents == null ? 0 : documents.size());
        }

        @Override
        public void delete(List<String> idList) {
            log.debug("fallback VectorStore 忽略按 ID 删除，ids={}", idList == null ? 0 : idList.size());
        }

        @Override
        public void delete(Filter.Expression expression) {
            log.debug("fallback VectorStore 忽略条件删除，expression={}", expression);
        }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            log.warn("fallback VectorStore 返回空检索结果，query={}", request == null ? null : request.getQuery());
            return List.of();
        }
    }
}
