package com.codingagent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FileOperationParser {

    private static final Logger logger = LoggerFactory.getLogger(FileOperationParser.class);

    private static final Pattern FILE_BLOCK_PATTERN = Pattern.compile(
            "(?:FILE|CREATE|UPDATE):\\s*([^\\n]+)\\n```(?:\\w+)?\\n(.*?)```",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );

    private static final Pattern FILE_PATH_CONTENT_PATTERN = Pattern.compile(
            "(?:File|Path):\\s*`?([^`\\n]+)`?\\s*\\n+```(?:\\w+)?\\n(.*?)```",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );

    public List<FileWriterService.FileOperation> parseFileOperations(String aiResponse, String baseDirectory) {
        List<FileWriterService.FileOperation> operations = new ArrayList<>();

        Matcher matcher = FILE_BLOCK_PATTERN.matcher(aiResponse);
        while (matcher.find()) {
            String filePath = matcher.group(1).trim();
            String content = matcher.group(2).trim();

            filePath = resolveFilePath(filePath, baseDirectory);

            operations.add(FileWriterService.FileOperation.builder()
                    .filePath(filePath)
                    .content(content)
                    .operationType(FileWriterService.OperationType.CREATE)
                    .build());

            logger.debug("Parsed file operation: {}", filePath);
        }

        if (operations.isEmpty()) {
            matcher = FILE_PATH_CONTENT_PATTERN.matcher(aiResponse);
            while (matcher.find()) {
                String filePath = matcher.group(1).trim();
                String content = matcher.group(2).trim();

                filePath = resolveFilePath(filePath, baseDirectory);

                operations.add(FileWriterService.FileOperation.builder()
                        .filePath(filePath)
                        .content(content)
                        .operationType(FileWriterService.OperationType.CREATE)
                        .build());

                logger.debug("Parsed file operation (alt pattern): {}", filePath);
            }
        }

        logger.info("Parsed {} file operations from AI response", operations.size());
        return operations;
    }

    private String resolveFilePath(String filePath, String baseDirectory) {
        filePath = filePath.replace("`", "").trim();

        if (filePath.startsWith("/")) {
            return filePath;
        }

        if (baseDirectory != null && !baseDirectory.isEmpty()) {
            if (baseDirectory.endsWith("/")) {
                return baseDirectory + filePath;
            } else {
                return baseDirectory + "/" + filePath;
            }
        }

        return filePath;
    }

    public boolean containsFileOperations(String aiResponse) {
        return FILE_BLOCK_PATTERN.matcher(aiResponse).find() ||
               FILE_PATH_CONTENT_PATTERN.matcher(aiResponse).find();
    }
}
