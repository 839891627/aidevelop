package com.example.aidevelop.service.rag;

import com.example.aidevelop.config.RagProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VectorRetrievalServiceTest {

    @Test
    void shouldBuildSearchRequestFromExplicitOptions() {
        Document document = new Document("贷款产品说明");
        RecordingVectorStore vectorStore = new RecordingVectorStore(List.of(document));

        RagProperties ragProperties = new RagProperties();
        ragProperties.setTopK(5);
        ragProperties.setSimilarityThreshold(0.2);

        VectorRetrievalService service = new VectorRetrievalService(vectorStore, ragProperties);

        List<Document> results = service.search("贷款额度", 3, 0.35);

        SearchRequest request = vectorStore.lastRequest();

        assertEquals("贷款额度", request.getQuery());
        assertEquals(3, request.getTopK());
        assertEquals(0.35, request.getSimilarityThreshold());
        assertEquals(List.of(document), results);
    }

    @Test
    void shouldUseRagDefaultsWhenOptionsAreNotProvided() {
        RecordingVectorStore vectorStore = new RecordingVectorStore(List.of());

        RagProperties ragProperties = new RagProperties();
        ragProperties.setTopK(7);
        ragProperties.setSimilarityThreshold(0.42);

        VectorRetrievalService service = new VectorRetrievalService(vectorStore, ragProperties);

        service.search("逾期规则");

        SearchRequest request = vectorStore.lastRequest();

        assertEquals("逾期规则", request.getQuery());
        assertEquals(7, request.getTopK());
        assertEquals(0.42, request.getSimilarityThreshold());
    }

    private static class RecordingVectorStore implements VectorStore {
        private final List<Document> documents;
        private SearchRequest lastRequest;

        private RecordingVectorStore(List<Document> documents) {
            this.documents = documents;
        }

        @Override
        public void add(List<Document> documents) {
        }

        @Override
        public void delete(List<String> idList) {
        }

        @Override
        public void delete(Filter.Expression expression) {
        }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            this.lastRequest = request;
            return new ArrayList<>(documents);
        }

        private SearchRequest lastRequest() {
            return lastRequest;
        }
    }
}
