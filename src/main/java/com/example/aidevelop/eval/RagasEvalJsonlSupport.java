package com.example.aidevelop.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * RAG 评测集与评测结果的 JSONL 读写工具。
 */
public final class RagasEvalJsonlSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private RagasEvalJsonlSupport() {
    }

    public static List<RagasEvalSample> readSamples(Path path) throws IOException {
        try (var lines = Files.lines(path, StandardCharsets.UTF_8)) {
            return lines
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("#"))
                .map(RagasEvalJsonlSupport::readSample)
                .toList();
        }
    }

    public static void writeResults(Path path, List<RagasEvalResult> results) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (RagasEvalResult result : results) {
                writer.write(OBJECT_MAPPER.writeValueAsString(result));
                writer.newLine();
            }
        }
    }

    private static RagasEvalSample readSample(String line) {
        try {
            return OBJECT_MAPPER.readValue(line, RagasEvalSample.class);
        } catch (IOException ex) {
            throw new IllegalArgumentException("评测集 JSONL 行解析失败: " + line, ex);
        }
    }
}
