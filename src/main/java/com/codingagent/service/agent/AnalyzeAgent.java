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
        logger.info("🔍 AnalyzeAgent starting code analysis...");
        
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(SYSTEM_PROMPT);
        String systemMessage = systemPromptTemplate.render();
        
        StringBuilder fullPromptText = new StringBuilder(systemMessage);
        
        if (directoryContext != null && !directoryContext.isEmpty()) {
            fullPromptText.append("\n\n").append(directoryContext);
        }
        
        fullPromptText.append("\n\nUser request: ").append(prompt);
        
        logger.info("⏳ Streaming AI analysis...");
        Prompt fullPrompt = new Prompt(fullPromptText.toString());
        
        StringBuilder result = new StringBuilder();
        StringBuilder lineBuffer = new StringBuilder();
        
        chatModel.stream(fullPrompt).doOnNext(chatResponse -> {
            String content = chatResponse.getResult().getOutput().getContent();
            result.append(content);
            lineBuffer.append(content);
            
            if (lineBuffer.toString().contains("\n")) {
                String[] lines = lineBuffer.toString().split("\n", -1);
                for (int i = 0; i < lines.length - 1; i++) {
                    String line = lines[i];
                    if (line.length() > 120) {
                        line = line.substring(0, 120) + "...";
                    }
                    logger.info("  💡 {}", line);
                }
                lineBuffer.setLength(0);
                lineBuffer.append(lines[lines.length - 1]);
            }
        }).blockLast();
        
        if (lineBuffer.length() > 0) {
            String line = lineBuffer.toString();
            if (line.length() > 120) {
                line = line.substring(0, 120) + "...";
            }
            logger.info("  💡 {}", line);
        }
        
        String finalResult = result.toString();
        logger.info("✓ Analysis complete (length: {} chars)", finalResult.length());
        return finalResult;
    }
}
