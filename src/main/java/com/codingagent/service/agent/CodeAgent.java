package com.codingagent.service.agent;

import com.codingagent.model.AgentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Service
public class CodeAgent implements Agent {

    private static final Logger logger = LoggerFactory.getLogger(CodeAgent.class);

    private final ChatModel chatModel;

    private static final String SYSTEM_PROMPT = """
            You are a code generation expert. Your role is to:
            - Write clean, efficient, and well-documented code
            - Follow best practices and design patterns
            - Generate production-ready code with proper error handling
            - Include necessary imports and dependencies
            - Provide explanations for complex implementations
            
            Always write idiomatic code for the target language with proper formatting.
            
            IMPORTANT: When creating or modifying files, use this format:
            
            FILE: path/to/file.ext
            ```language
            file content here
            ```
            
            For example:
            FILE: src/main/java/com/example/MyClass.java
            ```java
            package com.example;
            
            public class MyClass {
                // code here
            }
            ```
            
            You can specify multiple files in a single response.
            """;

    public CodeAgent(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public AgentType getType() {
        return AgentType.CODE;
    }

    @Override
    public String execute(String prompt, String directoryContext) {
        logger.info("🤖 CodeAgent starting execution...");
        logger.info("Request: {}", truncate(prompt, 100));
        
        StringBuilder fullPromptText = new StringBuilder(SYSTEM_PROMPT);
        
        if (directoryContext != null && !directoryContext.isEmpty()) {
            fullPromptText.append("\n\n").append(directoryContext);
        }
        
        fullPromptText.append("\n\nUser request: ").append(prompt);
        fullPromptText.append("\n\nRemember to use the FILE: format for any files you create or modify.");
        
        logger.info("⏳ Streaming AI response...");
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
                    logger.info("  💭 {}", line);
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
            logger.info("  💭 {}", line);
        }
        
        String finalResult = result.toString();
        logger.info("✓ AI response complete (length: {} chars)", finalResult.length());
        return finalResult;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

}
