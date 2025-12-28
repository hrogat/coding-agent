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

    public OrchestratorService(ClassificationAgent classificationAgent,
                               CollaborationAgent collaborationAgent,
                               List<Agent> agentList,
                               FileSystemService fileSystemService) {
        this.classificationAgent = classificationAgent;
        this.collaborationAgent = collaborationAgent;
        this.fileSystemService = fileSystemService;
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
        
        TaskSummary summary = extractTaskSummary(result);

        return AgentResponse.builder()
                .agentType(selectedType)
                .result(result)
                .reasoning("Request classified as " + selectedType + " task")
                .filesWritten(summary.filesWritten)
                .fileCount(summary.fileCount)
                .build();
    }

    private AgentResponse processWithCollaboration(String userPrompt, String directoryPath) {
        String directoryContext = buildDirectoryContext(directoryPath);
        
        CollaborationAgent.CollaborationResult collaborationResult = 
                collaborationAgent.executeCollaborativeProcess(userPrompt, directoryContext);
        
        TaskSummary summary = extractTaskSummary(collaborationResult.getCombinedResult());

        return AgentResponse.builder()
                .agentType(AgentType.CODE)
                .result(collaborationResult.getCombinedResult())
                .reasoning("Collaborative process: Code Generation → Analysis → Refinement")
                .filesWritten(summary.filesWritten)
                .fileCount(summary.fileCount)
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

    private TaskSummary extractTaskSummary(String result) {
        List<String> filesWritten = result.lines()
                .filter(line -> line.contains("Success: File written to"))
                .map(line -> {
                    int idx = line.indexOf("Success: File written to");
                    return line.substring(idx + 25).trim();
                })
                .collect(Collectors.toList());
        
        Integer fileCount = filesWritten.isEmpty() ? 0 : filesWritten.size();
        
        return new TaskSummary(filesWritten.isEmpty() ? null : filesWritten, fileCount);
    }

    private static class TaskSummary {
        final List<String> filesWritten;
        final Integer fileCount;

        TaskSummary(List<String> filesWritten, Integer fileCount) {
            this.filesWritten = filesWritten;
            this.fileCount = fileCount;
        }
    }
}
