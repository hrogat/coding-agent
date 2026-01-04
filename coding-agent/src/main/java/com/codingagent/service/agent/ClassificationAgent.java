package com.codingagent.service.agent;

import com.codingagent.model.AgentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Service
public class ClassificationAgent {

    private static final Logger logger = LoggerFactory.getLogger(ClassificationAgent.class);
    private final ChatModel chatModel;

    private static final String CLASSIFICATION_PROMPT = """
            You are a task classifier. Analyze the following user request and determine which type of coding agent should handle it.
            
            Available agent types:
            - ANALYZE: For code analysis, review, quality assessment, architecture evaluation, identifying issues
            - CODE: For generating new code, implementing features, creating functions/classes
            - BUGFIX: For debugging, fixing errors, resolving issues in existing code
            
            User request: {prompt}
            
            Respond with ONLY one word: ANALYZE, CODE, or BUGFIX
            """;

    public ClassificationAgent(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public AgentType classify(String userPrompt) {
        logger.info("Classifying request...");
        
        String classificationPromptText = CLASSIFICATION_PROMPT.replace("{prompt}", userPrompt);
        Prompt prompt = new Prompt(classificationPromptText);
        
        String response = chatModel.call(prompt).getResult().getOutput().getContent().trim().toUpperCase();
        logger.debug("Classification response: {}", response);

        try {
            AgentType agentType = AgentType.valueOf(response);
            logger.info("Classified as: {}", agentType);
            return agentType;
        } catch (IllegalArgumentException e) {
            logger.warn("Unable to parse agent type from response: {}. Defaulting to CODE", response);
            return AgentType.CODE;
        }
    }
}
