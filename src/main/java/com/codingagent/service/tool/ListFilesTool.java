package com.codingagent.service.tool;

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
        String cleaned = parameters.trim();
        if (cleaned.startsWith("{") && cleaned.contains("\"path\"")) {
            int start = cleaned.indexOf("\"path\"");
            int colonIdx = cleaned.indexOf(":", start);
            int valueStart = cleaned.indexOf("\"", colonIdx) + 1;
            int valueEnd = cleaned.indexOf("\"", valueStart);
            return cleaned.substring(valueStart, valueEnd);
        }
        return cleaned;
    }
}
