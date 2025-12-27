package com.codingagent.service;

import com.codingagent.config.FileSystemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileSystemService {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemService.class);
    private final FileSystemProperties properties;

    public FileSystemService(FileSystemProperties properties) {
        this.properties = properties;
    }

    public String buildDirectoryContext(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            return "";
        }

        Path path = Paths.get(directoryPath);
        if (!Files.exists(path)) {
            logger.warn("Directory does not exist: {}", directoryPath);
            return "Directory does not exist: " + directoryPath;
        }

        if (!Files.isDirectory(path)) {
            logger.warn("Path is not a directory: {}", directoryPath);
            return "Path is not a directory: " + directoryPath;
        }

        StringBuilder context = new StringBuilder();
        context.append("Directory Context: ").append(directoryPath).append("\n\n");

        try {
            context.append("Directory Structure:\n");
            context.append(buildDirectoryTree(path, 0, properties.getMaxDepth()));
            context.append("\n\n");

            context.append("File Contents:\n");
            List<Path> files = listFiles(path);
            
            int fileCount = 0;
            for (Path file : files) {
                if (fileCount >= properties.getMaxFiles()) {
                    context.append("\n[Additional files omitted - limit reached]\n");
                    break;
                }
                
                String fileContent = readFileContent(file);
                if (fileContent != null) {
                    context.append("--- File: ").append(path.relativize(file)).append(" ---\n");
                    context.append(fileContent).append("\n\n");
                    fileCount++;
                }
            }

        } catch (IOException e) {
            logger.error("Error reading directory: {}", directoryPath, e);
            context.append("Error reading directory: ").append(e.getMessage());
        }

        return context.toString();
    }

    private String buildDirectoryTree(Path directory, int depth, int maxDepth) throws IOException {
        if (depth > maxDepth) {
            return "";
        }

        StringBuilder tree = new StringBuilder();
        String indent = "  ".repeat(depth);

        try (Stream<Path> paths = Files.list(directory)) {
            List<Path> sortedPaths = paths.sorted().collect(Collectors.toList());
            
            for (Path path : sortedPaths) {
                String fileName = path.getFileName().toString();
                
                if (shouldSkipPath(fileName)) {
                    continue;
                }

                tree.append(indent).append("├── ").append(fileName);
                
                if (Files.isDirectory(path)) {
                    tree.append("/\n");
                    tree.append(buildDirectoryTree(path, depth + 1, maxDepth));
                } else {
                    tree.append("\n");
                }
            }
        }

        return tree.toString();
    }

    private List<Path> listFiles(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> !shouldSkipPath(path.getFileName().toString()))
                    .filter(path -> isTextFile(path))
                    .limit(properties.getMaxFiles())
                    .collect(Collectors.toList());
        }
    }

    private String readFileContent(Path file) {
        try {
            long fileSize = Files.size(file);
            if (fileSize > properties.getMaxFileSize()) {
                logger.debug("Skipping large file: {} (size: {} bytes)", file, fileSize);
                return null;
            }

            return Files.readString(file);
        } catch (IOException e) {
            logger.warn("Could not read file: {}", file, e);
            return null;
        }
    }

    private boolean shouldSkipPath(String fileName) {
        return fileName.startsWith(".") ||
               fileName.equals("target") ||
               fileName.equals("build") ||
               fileName.equals("node_modules") ||
               fileName.equals("dist") ||
               fileName.equals("out") ||
               fileName.endsWith(".class") ||
               fileName.endsWith(".jar") ||
               fileName.endsWith(".war");
    }

    private boolean isTextFile(Path file) {
        String fileName = file.getFileName().toString();
        String lowerCase = fileName.toLowerCase();
        
        return lowerCase.endsWith(".java") ||
               lowerCase.endsWith(".xml") ||
               lowerCase.endsWith(".yml") ||
               lowerCase.endsWith(".yaml") ||
               lowerCase.endsWith(".properties") ||
               lowerCase.endsWith(".txt") ||
               lowerCase.endsWith(".md") ||
               lowerCase.endsWith(".json") ||
               lowerCase.endsWith(".js") ||
               lowerCase.endsWith(".ts") ||
               lowerCase.endsWith(".py") ||
               lowerCase.endsWith(".sh") ||
               lowerCase.endsWith(".sql") ||
               lowerCase.endsWith(".html") ||
               lowerCase.endsWith(".css") ||
               lowerCase.endsWith(".kt") ||
               lowerCase.endsWith(".gradle");
    }
}
