package com.codingagent.service.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class ReadFileTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(ReadFileTool.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_FILE_SIZE = 1024 * 1024; // 1MB

    @Override
    public String getName() {
        return "read_file";
    }

    @Override
    public String getDescription() {
        return """
                Reads the content of a file.
                Parameters: {"path": "file/path"}
                Returns: File content as string.
                """;
    }

    @Override
    public String execute(String parameters) {
        try {
            String path = extractPath(parameters);
            Path file = Paths.get(path);

            if (!Files.exists(file)) {
                return "Error: File does not exist: " + path;
            }

            if (!Files.isRegularFile(file)) {
                return "Error: Path is not a file: " + path;
            }

            long fileSize = Files.size(file);
            if (fileSize > MAX_FILE_SIZE) {
                return "Error: File too large (max 1MB): " + path;
            }

            String content = Files.readString(file);
            logger.info("Read file: {} ({} bytes)", path, fileSize);
            return content;

        } catch (IOException e) {
            logger.error("Error reading file", e);
            return "Error: " + e.getMessage();
        }
    }

    private String extractPath(String parameters) {
        try {
            JsonNode jsonNode = objectMapper.readTree(parameters.trim());
            return jsonNode.has("path") ? jsonNode.get("path").asText() : parameters.trim();
        } catch (Exception e) {
            logger.debug("Failed to parse as JSON, using raw parameter: {}", parameters);
            return parameters.trim();
        }
    }
}
