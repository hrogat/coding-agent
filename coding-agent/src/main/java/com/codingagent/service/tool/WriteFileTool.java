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
import java.nio.file.StandardOpenOption;

@Component
public class WriteFileTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(WriteFileTool.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "write_file";
    }

    @Override
    public String getDescription() {
        return """
                Writes content to a file. Creates parent directories if needed.
                Parameters: {"path": "file/path", "content": "file content"}
                Returns: Success message or error.
                """;
    }

    @Override
    public String execute(String parameters) {
        try {
            WriteFileParams params = extractParams(parameters);
            
            // Resolve path relative to base directory if available
            String baseDir = ToolExecutionContext.getBaseDirectory();
            Path file;
            if (baseDir != null && !baseDir.trim().isEmpty()) {
                file = Paths.get(baseDir, params.path);
                logger.debug("Resolving path '{}' relative to base directory '{}' -> '{}'", 
                        params.path, baseDir, file.toAbsolutePath());
            } else {
                file = Paths.get(params.path);
                logger.debug("Using path '{}' without base directory", params.path);
            }

            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            
            Files.writeString(file, params.content, 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.TRUNCATE_EXISTING);

            logger.info("Wrote file: {} ({} bytes)", file.toAbsolutePath(), params.content.length());
            return "Success: File written to " + file.toAbsolutePath();

        } catch (IOException e) {
            logger.error("Error writing file", e);
            return "Error: " + e.getMessage();
        }
    }

    private WriteFileParams extractParams(String parameters) {
        try {
            logger.debug("Parsing parameters: {}", parameters);
            JsonNode jsonNode = objectMapper.readTree(parameters.trim());
            String path = jsonNode.has("path") ? jsonNode.get("path").asText() : "";
            String content = jsonNode.has("content") ? jsonNode.get("content").asText() : "";
            logger.debug("Extracted path: '{}', content length: {}", path, content.length());
            return new WriteFileParams(path, content);
        } catch (Exception e) {
            logger.error("Failed to parse parameters: {}", parameters, e);
            return new WriteFileParams("", "");
        }
    }

    private static class WriteFileParams {
        final String path;
        final String content;

        WriteFileParams(String path, String content) {
            this.path = path;
            this.content = content;
        }
    }
}
