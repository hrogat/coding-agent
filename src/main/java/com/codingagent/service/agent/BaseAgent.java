package com.codingagent.service.agent;

import org.slf4j.Logger;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

public abstract class BaseAgent implements Agent {

    protected final ChatModel chatModel;

    protected BaseAgent(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String execute(String prompt, String directoryContext) {
        Logger logger = getLogger();
        logger.info("{} starting execution...", getLogPrefix());
        
        String fullPromptText = buildFullPrompt(prompt, directoryContext);
        
        logger.info("⏳ Streaming AI response...");
        Prompt fullPrompt = new Prompt(fullPromptText);
        
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
                    logger.info("  {} {}", getLogEmoji(), line);
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
            logger.info("  {} {}", getLogEmoji(), line);
        }
        
        String finalResult = result.toString();
        logger.info("✓ {} complete (length: {} chars)", getType(), finalResult.length());
        return finalResult;
    }

    protected String buildFullPrompt(String prompt, String directoryContext) {
        StringBuilder fullPromptText = new StringBuilder(getSystemPrompt());
        
        if (directoryContext != null && !directoryContext.isEmpty()) {
            fullPromptText.append("\n\n").append(directoryContext);
        }
        
        fullPromptText.append("\n\nUser request: ").append(prompt);
        
        return fullPromptText.toString();
    }

    protected abstract Logger getLogger();
    
    protected abstract String getSystemPrompt();
    
    protected abstract String getLogPrefix();
    
    protected abstract String getLogEmoji();
}
