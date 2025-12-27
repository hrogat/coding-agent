package com.codingagent.service.agent;

import com.codingagent.model.AgentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service
public class AnalyzeAgent extends BaseAgent {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzeAgent.class);

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
        super(chatModel);
    }

    @Override
    public AgentType getType() {
        return AgentType.ANALYZE;
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
        return "🔍 AnalyzeAgent";
    }

    @Override
    protected String getLogEmoji() {
        return "💡";
    }
}
