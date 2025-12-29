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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ListFilesTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(ListFilesTool.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "list_files";
    }

    @Override
    public String getDescription() {
        return """
                Lists files and directories in a given path.
                Parameters: {"path": "directory/path"}
                Returns: List of files and directories with their types.
                """;
    }

    @Override
    public String execute(String parameters) {
        try {
            String path = extractPath(parameters);
            
            // Resolve path relative to base directory if available
            String baseDir = ToolExecutionContext.getBaseDirectory();
            Path directory;
            if (baseDir != null && !baseDir.trim().isEmpty()) {
                directory = Paths.get(baseDir, path.isEmpty() ? "." : path);
                logger.debug("Resolving path '{}' relative to base directory '{}' -> '{}'", 
                        path, baseDir, directory.toAbsolutePath());
            } else {
                directory = Paths.get(path.isEmpty() ? "." : path);
                logger.debug("Using path '{}' without base directory", path);
            }

            if (!Files.exists(directory)) {
                logger.warn("Directory not found: {}", directory.toAbsolutePath());
                return "Error: Directory not found: " + directory.toAbsolutePath();
            }

            if (!Files.isDirectory(directory)) {
                logger.warn("Path is not a directory: {}", directory.toAbsolutePath());
                return "Error: Not a directory: " + directory.toAbsolutePath();
            }

            StringBuilder result = new StringBuilder();
            result.append("Files in ").append(directory.toAbsolutePath()).append(":\n");

            Files.list(directory)
                    .sorted()
                    .forEach(file -> {
                        String type = Files.isDirectory(file) ? "[DIR]" : "[FILE]";
                        result.append(type).append(" ").append(file.getFileName()).append("\n");
                    });

            String output = result.toString();
            logger.debug("Listed directory: {} ({} items)", directory.toAbsolutePath(), Files.list(directory).count());
            return output;

        } catch (IOException e) {
            logger.error("Error listing directory", e);
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
