package com.codingagent.service.tool;

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
            Path file = Paths.get(params.path);

            Files.createDirectories(file.getParent());
            Files.writeString(file, params.content, 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.TRUNCATE_EXISTING);

            logger.info("Wrote file: {} ({} bytes)", params.path, params.content.length());
            return "Success: File written to " + params.path;

        } catch (IOException e) {
            logger.error("Error writing file", e);
            return "Error: " + e.getMessage();
        }
    }

    private WriteFileParams extractParams(String parameters) {
        String cleaned = parameters.trim();
        String path = "";
        String content = "";

        if (cleaned.startsWith("{")) {
            int pathStart = cleaned.indexOf("\"path\"");
            if (pathStart != -1) {
                int colonIdx = cleaned.indexOf(":", pathStart);
                int valueStart = cleaned.indexOf("\"", colonIdx) + 1;
                int valueEnd = cleaned.indexOf("\"", valueStart);
                path = cleaned.substring(valueStart, valueEnd);
            }

            int contentStart = cleaned.indexOf("\"content\"");
            if (contentStart != -1) {
                int colonIdx = cleaned.indexOf(":", contentStart);
                int valueStart = cleaned.indexOf("\"", colonIdx) + 1;
                int valueEnd = cleaned.lastIndexOf("\"");
                content = cleaned.substring(valueStart, valueEnd);
                content = content.replace("\\n", "\n").replace("\\t", "\t");
            }
        }

        return new WriteFileParams(path, content);
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
