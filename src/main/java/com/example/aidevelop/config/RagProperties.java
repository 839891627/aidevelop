package com.example.aidevelop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.chat.rag")
public class RagProperties {

    private boolean enabled = true;
    private double similarityThreshold = 0.2;
    private int topK = 5;
    private Chunking chunking = new Chunking();

    @Data
    public static class Chunking {
        private int chunkSize = 500;
        private int minChunkSizeChars = 200;
        private int minChunkLengthToEmbed = 10;
        private int maxNumChunks = 10000;
        private boolean keepSeparator = true;
    }
}