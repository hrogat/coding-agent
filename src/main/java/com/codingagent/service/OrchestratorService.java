package com.codingagent.service;

import com.codingagent.model.AgentResponse;
import com.codingagent.model.AgentType;
import com.codingagent.service.agent.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrchestratorService {

    private static final Logger logger = LoggerFactory.getLogger(OrchestratorService.class);

    private final ChatModel chatModel;
    private final Map<AgentType, Agent> agents;
    private final FileSystemService fileSystemService;
    private final FileWriterService fileWriterService;
    private final FileOperationParser fileOperationParser;

    private static final String CLASSIFICATION_PROMPT = """
            You are a task classifier. Analyze the following user request and determine which type of coding agent should handle it.
            
            Available agent types:
            - ANALYZE: For code analysis, review, quality assessment, architecture evaluation, identifying issues
            - CODE: For generating new code, implementing features, creating functions/classes
            - BUGFIX: For debugging, fixing errors, resolving issues in existing code
            
            User request: {prompt}
            
            Respond with ONLY one word: ANALYZE, CODE, or BUGFIX
            """;

    public OrchestratorService(ChatModel chatModel, List<Agent> agentList, FileSystemService fileSystemService,
                               FileWriterService fileWriterService, FileOperationParser fileOperationParser) {
        this.chatModel = chatModel;
        this.fileSystemService = fileSystemService;
        this.fileWriterService = fileWriterService;
        this.fileOperationParser = fileOperationParser;
        this.agents = agentList.stream()
                .collect(Collectors.toMap(Agent::getType, Function.identity()));
        logger.info("OrchestratorService initialized with {} agents", agents.size());
    }

    public AgentResponse processRequest(String userPrompt, String directoryPath) {
        return processRequest(userPrompt, directoryPath, false);
    }

    public AgentResponse processRequest(String userPrompt, String directoryPath, Boolean useCollaboration) {
        logger.info("Processing request: {} (collaboration: {})", userPrompt, useCollaboration);

        AgentType selectedType = classifyRequest(userPrompt);
        logger.info("Selected agent type: {}", selectedType);

        if (Boolean.TRUE.equals(useCollaboration) && selectedType == AgentType.CODE) {
            return processWithCollaboration(userPrompt, directoryPath);
        }

        Agent selectedAgent = agents.get(selectedType);
        if (selectedAgent == null) {
            logger.error("No agent found for type: {}", selectedType);
            throw new IllegalStateException("Agent not found for type: " + selectedType);
        }

        String directoryContext = "";
        if (directoryPath != null && !directoryPath.trim().isEmpty()) {
            logger.info("Building directory context for: {}", directoryPath);
            directoryContext = fileSystemService.buildDirectoryContext(directoryPath);
        }

        String result = selectedAgent.execute(userPrompt, directoryContext);

        List<String> filesWritten = null;
        Integer fileCount = 0;

        if (fileOperationParser.containsFileOperations(result)) {
            logger.info("📄 Detected file operations in AI response, processing...");
            List<FileWriterService.FileOperation> operations = fileOperationParser.parseFileOperations(result, directoryPath);
            
            if (!operations.isEmpty()) {
                logger.info("📝 Writing {} file(s)...", operations.size());
                List<FileWriterService.FileOperation> results = fileWriterService.writeFiles(operations);
                filesWritten = results.stream()
                        .filter(FileWriterService.FileOperation::isSuccess)
                        .map(FileWriterService.FileOperation::getFilePath)
                        .collect(Collectors.toList());
                fileCount = filesWritten.size();
                
                logger.info("✅ Successfully wrote {} files", fileCount);
            }
        }

        return AgentResponse.builder()
                .agentType(selectedType)
                .result(result)
                .reasoning("Request classified as " + selectedType + " task")
                .filesWritten(filesWritten)
                .fileCount(fileCount)
                .build();
    }

    private AgentType classifyRequest(String userPrompt) {
        String classificationPromptText = CLASSIFICATION_PROMPT.replace("{prompt}", userPrompt);
        Prompt prompt = new Prompt(classificationPromptText);
        
        String response = chatModel.call(prompt).getResult().getOutput().getContent().trim().toUpperCase();
        logger.debug("Classification response: {}", response);

        try {
            return AgentType.valueOf(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Unable to parse agent type from response: {}. Defaulting to CODE", response);
            return AgentType.CODE;
        }
    }

    private AgentResponse processWithCollaboration(String userPrompt, String directoryPath) {
        logger.info("Starting collaborative code generation process");

        String directoryContext = "";
        if (directoryPath != null && !directoryPath.trim().isEmpty()) {
            logger.info("Building directory context for: {}", directoryPath);
            directoryContext = fileSystemService.buildDirectoryContext(directoryPath);
        }

        Agent codeAgent = agents.get(AgentType.CODE);
        Agent analyzeAgent = agents.get(AgentType.ANALYZE);

        logger.info("Step 1/3: Generating initial code");
        String initialCode = codeAgent.execute(userPrompt, directoryContext);

        logger.info("Step 2/3: Analyzing generated code");
        String analysisPrompt = String.format("""
            Analyze the following code for:
            - Code quality and best practices
            - Security vulnerabilities
            - Performance issues
            - Potential bugs
            - Design patterns and architecture
            
            Provide specific, actionable feedback for improvements.
            
            Code to analyze:
            %s
            """, initialCode);
        String analysis = analyzeAgent.execute(analysisPrompt, null);

        logger.info("Step 3/3: Refining code based on analysis");
        String refinementPrompt = String.format("""
            Improve the following code based on this analysis feedback.
            Apply all suggested improvements and maintain the FILE: format for file operations.
            
            Analysis feedback:
            %s
            
            Original code:
            %s
            
            Provide the improved code with all files in the FILE: format.
            """, analysis, initialCode);
        String refinedCode = codeAgent.execute(refinementPrompt, directoryContext);

        List<String> filesWritten = null;
        Integer fileCount = 0;

        if (fileOperationParser.containsFileOperations(refinedCode)) {
            logger.info("📄 Detected file operations in refined code, processing...");
            List<FileWriterService.FileOperation> operations = fileOperationParser.parseFileOperations(refinedCode, directoryPath);
            
            if (!operations.isEmpty()) {
                logger.info("📝 Writing {} file(s) after collaboration...", operations.size());
                List<FileWriterService.FileOperation> results = fileWriterService.writeFiles(operations);
                filesWritten = results.stream()
                        .filter(FileWriterService.FileOperation::isSuccess)
                        .map(FileWriterService.FileOperation::getFilePath)
                        .collect(Collectors.toList());
                fileCount = filesWritten.size();
                
                logger.info("✅ Successfully wrote {} files after collaboration", fileCount);
            }
        }

        String combinedResult = refinedCode + "\n\n--- Analysis Report ---\n" + analysis;

        return AgentResponse.builder()
                .agentType(AgentType.CODE)
                .result(combinedResult)
                .reasoning("Collaborative process: Code Generation → Analysis → Refinement")
                .filesWritten(filesWritten)
                .fileCount(fileCount)
                .build();
    }
}
