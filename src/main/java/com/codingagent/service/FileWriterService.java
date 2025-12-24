package com.codingagent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileWriterService {

    private static final Logger logger = LoggerFactory.getLogger(FileWriterService.class);

    public List<FileOperation> writeFiles(List<FileOperation> operations) {
        List<FileOperation> results = new ArrayList<>();

        for (FileOperation operation : operations) {
            try {
                FileOperation result = executeOperation(operation);
                results.add(result);
            } catch (Exception e) {
                logger.error("Failed to execute file operation: {}", operation.getFilePath(), e);
                results.add(FileOperation.builder()
                        .filePath(operation.getFilePath())
                        .content(operation.getContent())
                        .operationType(operation.getOperationType())
                        .success(false)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        return results;
    }

    private FileOperation executeOperation(FileOperation operation) throws IOException {
        Path path = Paths.get(operation.getFilePath());

        switch (operation.getOperationType()) {
            case CREATE:
                return createFile(path, operation.getContent());
            case UPDATE:
                return updateFile(path, operation.getContent());
            case DELETE:
                return deleteFile(path);
            default:
                throw new IllegalArgumentException("Unknown operation type: " + operation.getOperationType());
        }
    }

    private FileOperation createFile(Path path, String content) throws IOException {
        if (Files.exists(path)) {
            logger.warn("File already exists, will overwrite: {}", path);
        }

        logger.info("Creating file: {} (size: {} bytes)", path, content.length());
        logContentPreview(path.toString(), content);

        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        logger.info("✓ Created file: {}", path);
        return FileOperation.builder()
                .filePath(path.toString())
                .content(content)
                .operationType(OperationType.CREATE)
                .success(true)
                .build();
    }

    private FileOperation updateFile(Path path, String content) throws IOException {
        if (!Files.exists(path)) {
            logger.warn("File does not exist, will create: {}", path);
            return createFile(path, content);
        }

        logger.info("Updating file: {} (size: {} bytes)", path, content.length());
        logContentPreview(path.toString(), content);

        Files.writeString(path, content, StandardOpenOption.TRUNCATE_EXISTING);

        logger.info("✓ Updated file: {}", path);
        return FileOperation.builder()
                .filePath(path.toString())
                .content(content)
                .operationType(OperationType.UPDATE)
                .success(true)
                .build();
    }

    private FileOperation deleteFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            logger.warn("File does not exist, cannot delete: {}", path);
            return FileOperation.builder()
                    .filePath(path.toString())
                    .operationType(OperationType.DELETE)
                    .success(false)
                    .errorMessage("File does not exist")
                    .build();
        }

        Files.delete(path);

        logger.info("✓ Deleted file: {}", path);
        return FileOperation.builder()
                .filePath(path.toString())
                .operationType(OperationType.DELETE)
                .success(true)
                .build();
    }

    public static class FileOperation {
        private String filePath;
        private String content;
        private OperationType operationType;
        private boolean success;
        private String errorMessage;

        public static FileOperationBuilder builder() {
            return new FileOperationBuilder();
        }

        public String getFilePath() {
            return filePath;
        }

        public String getContent() {
            return content;
        }

        public OperationType getOperationType() {
            return operationType;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public static class FileOperationBuilder {
            private String filePath;
            private String content;
            private OperationType operationType;
            private boolean success;
            private String errorMessage;

            public FileOperationBuilder filePath(String filePath) {
                this.filePath = filePath;
                return this;
            }

            public FileOperationBuilder content(String content) {
                this.content = content;
                return this;
            }

            public FileOperationBuilder operationType(OperationType operationType) {
                this.operationType = operationType;
                return this;
            }

            public FileOperationBuilder success(boolean success) {
                this.success = success;
                return this;
            }

            public FileOperationBuilder errorMessage(String errorMessage) {
                this.errorMessage = errorMessage;
                return this;
            }

            public FileOperation build() {
                FileOperation operation = new FileOperation();
                operation.filePath = this.filePath;
                operation.content = this.content;
                operation.operationType = this.operationType;
                operation.success = this.success;
                operation.errorMessage = this.errorMessage;
                return operation;
            }
        }
    }

    public enum OperationType {
        CREATE,
        UPDATE,
        DELETE
    }

    private void logContentPreview(String filePath, String content) {
        String[] lines = content.split("\n", 6);
        int previewLines = Math.min(5, lines.length);
        StringBuilder preview = new StringBuilder();
        preview.append("Preview (first ").append(previewLines).append(" lines):\n");
        for (int i = 0; i < previewLines; i++) {
            String line = lines[i];
            if (line.length() > 100) {
                line = line.substring(0, 100) + "...";
            }
            preview.append("  ").append(line).append("\n");
        }
        if (lines.length > 5) {
            preview.append("  ... (").append(lines.length - 5).append(" more lines)");
        }
        logger.info(preview.toString());
    }
}
