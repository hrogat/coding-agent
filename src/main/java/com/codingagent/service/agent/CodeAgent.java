package com.codingagent.service.agent;

import com.codingagent.model.AgentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service
public class CodeAgent extends BaseAgent {

    private static final Logger logger = LoggerFactory.getLogger(CodeAgent.class);

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
        super(chatModel);
    }

    @Override
    public AgentType getType() {
        return AgentType.CODE;
    }

    @Override
    protected String buildFullPrompt(String prompt, String directoryContext) {
        String basePrompt = super.buildFullPrompt(prompt, directoryContext);
        return basePrompt + "\n\nRemember to use the FILE: format for any files you create or modify.";
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected String getLogPrefix() {
        return "🤖 CodeAgent";
    }

    @Override
    protected String getLogEmoji() {
        return "💭";
    }

}
