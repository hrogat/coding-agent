package com.codingagent.service;

import com.codingagent.exception.AgentException;
import com.codingagent.model.AgentType;
import com.codingagent.model.StreamEvent;
import com.codingagent.service.agent.Agent;
import com.codingagent.service.agent.ClassificationAgent;
import com.codingagent.service.agent.CollaborationAgent;
import com.codingagent.service.agent.StreamingToolBasedAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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

    public Flux<StreamEvent> processRequestStream(String userPrompt, String directoryPath, Boolean useCollaboration) {
        logger.info("Processing streaming request: {} (collaboration: {})", userPrompt, useCollaboration);

        AgentType selectedType = classificationAgent.classify(userPrompt);
        
        if (Boolean.TRUE.equals(useCollaboration) && selectedType == AgentType.CODE) {
            String directoryContext = buildDirectoryContext(directoryPath);
            return collaborationAgent.executeCollaborativeStream(userPrompt, directoryContext, directoryPath);
        }
        
        Agent selectedAgent = getAgent(selectedType);
        
        if (selectedAgent instanceof StreamingToolBasedAgent streamCapableAgent) {
            String directoryContext = buildDirectoryContext(directoryPath);
            return streamCapableAgent.executeStream(userPrompt, directoryContext, directoryPath);
        }
        return Flux.error(new AgentException("Agent does not support streaming: " + selectedType));
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
}
