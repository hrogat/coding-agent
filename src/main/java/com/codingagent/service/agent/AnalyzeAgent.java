package com.codingagent.service.agent;

import com.codingagent.model.AgentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.stereotype.Service;

@Service
public class AnalyzeAgent implements Agent {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzeAgent.class);

    private final ChatModel chatModel;

    private static final String SYSTEM_PROMPT = """
            You are a code analysis expert. Your role is to:
            - Analyze code structure, patterns, and architecture
            - Identify code smells and potential issues
            - Provide insights on code quality and maintainability
            - Suggest improvements and best practices
            - Review code complexity and performance characteristics
            
            Provide detailed, actionable analysis with specific examples.
            """;

    public AnalyzeAgent(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public AgentType getType() {
        return AgentType.ANALYZE;
    }

    @Override
    public String execute(String prompt, String directoryContext) {
        logger.debug("AnalyzeAgent executing with prompt: {}", prompt);
        
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(SYSTEM_PROMPT);
        String systemMessage = systemPromptTemplate.render();
        
        StringBuilder fullPromptText = new StringBuilder(systemMessage);
        
        if (directoryContext != null && !directoryContext.isEmpty()) {
            fullPromptText.append("\n\n").append(directoryContext);
        }
        
        fullPromptText.append("\n\nUser request: ").append(prompt);
        
        Prompt fullPrompt = new Prompt(fullPromptText.toString());
        String result = chatModel.call(fullPrompt).getResult().getOutput().getContent();
        
        logger.debug("AnalyzeAgent result: {}", result);
        return result;
    }
}
