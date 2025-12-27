package com.codingagent.service;

import com.codingagent.exception.AgentException;
import com.codingagent.model.AgentResponse;
import com.codingagent.model.AgentType;
import com.codingagent.service.agent.Agent;
import com.codingagent.service.agent.ClassificationAgent;
import com.codingagent.service.agent.CollaborationAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrchestratorService {

    private static final Logger logger = LoggerFactory.getLogger(OrchestratorService.class);

    private final ClassificationAgent classificationAgent;
    private final CollaborationAgent collaborationAgent;
    private final Map<AgentType, Agent> agents;
    private final FileSystemService fileSystemService;
    private final FileWriterService fileWriterService;
    private final FileOperationParser fileOperationParser;

    public OrchestratorService(ClassificationAgent classificationAgent,
                               CollaborationAgent collaborationAgent,
                               List<Agent> agentList,
                               FileSystemService fileSystemService,
                               FileWriterService fileWriterService,
                               FileOperationParser fileOperationParser) {
        this.classificationAgent = classificationAgent;
        this.collaborationAgent = collaborationAgent;
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

        AgentType selectedType = classificationAgent.classify(userPrompt);

        if (Boolean.TRUE.equals(useCollaboration) && selectedType == AgentType.CODE) {
            return processWithCollaboration(userPrompt, directoryPath);
        }

        Agent selectedAgent = getAgent(selectedType);
        String directoryContext = buildDirectoryContext(directoryPath);
        String result = selectedAgent.execute(userPrompt, directoryContext);
        FileOperationResult fileResult = processFileOperations(result, directoryPath);

        return AgentResponse.builder()
                .agentType(selectedType)
                .result(result)
                .reasoning("Request classified as " + selectedType + " task")
                .filesWritten(fileResult.filesWritten)
                .fileCount(fileResult.fileCount)
                .build();
    }

    private AgentResponse processWithCollaboration(String userPrompt, String directoryPath) {
        String directoryContext = buildDirectoryContext(directoryPath);
        
        CollaborationAgent.CollaborationResult collaborationResult = 
                collaborationAgent.executeCollaborativeProcess(userPrompt, directoryContext);
        
        FileOperationResult fileResult = processFileOperations(
                collaborationResult.getRefinedCode(), directoryPath);

        return AgentResponse.builder()
                .agentType(AgentType.CODE)
                .result(collaborationResult.getCombinedResult())
                .reasoning("Collaborative process: Code Generation → Analysis → Refinement")
                .filesWritten(fileResult.filesWritten)
                .fileCount(fileResult.fileCount)
                .build();
    }

    private Agent getAgent(AgentType agentType) {
        Agent agent = agents.get(agentType);
        if (agent == null) {
            logger.error("No agent found for type: {}", agentType);
            throw new AgentException("Agent not found for type: " + agentType);
        }
        return agent;
    }

    private String buildDirectoryContext(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            return "";
        }
        logger.info("Building directory context for: {}", directoryPath);
        return fileSystemService.buildDirectoryContext(directoryPath);
    }

    private FileOperationResult processFileOperations(String result, String directoryPath) {
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

        return new FileOperationResult(filesWritten, fileCount);
    }

    private static class FileOperationResult {
        final List<String> filesWritten;
        final Integer fileCount;

        FileOperationResult(List<String> filesWritten, Integer fileCount) {
            this.filesWritten = filesWritten;
            this.fileCount = fileCount;
        }
    }
}
