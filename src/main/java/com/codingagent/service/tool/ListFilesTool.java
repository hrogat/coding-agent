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
            Path directory = Paths.get(path);

            if (!Files.exists(directory)) {
                return "Error: Directory does not exist: " + path;
            }

            if (!Files.isDirectory(directory)) {
                return "Error: Path is not a directory: " + path;
            }

            try (Stream<Path> paths = Files.list(directory)) {
                String listing = paths
                        .map(p -> {
                            String type = Files.isDirectory(p) ? "[DIR]" : "[FILE]";
                            return type + " " + p.getFileName().toString();
                        })
                        .collect(Collectors.joining("\n"));

                logger.info("Listed files in: {}", path);
                return listing.isEmpty() ? "Directory is empty" : listing;
            }

        } catch (IOException e) {
            logger.error("Error listing files", e);
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
