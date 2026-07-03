package com.example.aidevelop.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VectorIndexBuilderTest {

    @Test
    void shouldAddDocumentsInBatchesWithinEmbeddingInputLimit() {
        RagProperties ragProperties = new RagProperties();
        ragProperties.getChunking().setChunkSize(1000);
        ragProperties.getChunking().setMinChunkSizeChars(1);
        ragProperties.getChunking().setMinChunkLengthToEmbed(1);

        VectorIndexBuilder builder = new VectorIndexBuilder(ragProperties);
        ReflectionTestUtils.setField(builder, "textResources", textResources(12));
        ReflectionTestUtils.setField(builder, "pdfResources", new Resource[0]);

        RecordingVectorStore vectorStore = new RecordingVectorStore();

        builder.buildIndex(vectorStore);

        assertThat(vectorStore.batchSizes).containsExactly(10, 2);
    }

    private Resource[] textResources(int count) {
        Resource[] resources = new Resource[count];
        for (int i = 0; i < count; i++) {
            String filename = "doc-" + i + ".txt";
            String content = "知识库片段 " + i;
            resources[i] = new NamedByteArrayResource(filename, content.getBytes(StandardCharsets.UTF_8));
        }
        return resources;
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(String filename, byte[] byteArray) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }

    private static class RecordingVectorStore implements VectorStore {
        private final List<Integer> batchSizes = new ArrayList<>();

        @Override
        public void add(List<Document> documents) {
            batchSizes.add(documents.size());
        }

        @Override
        public void delete(List<String> idList) {
        }

        @Override
        public void delete(Filter.Expression expression) {
        }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            return List.of();
        }
    }
}
