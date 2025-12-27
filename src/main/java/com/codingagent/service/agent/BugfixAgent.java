package com.codingagent.service.agent;

import com.codingagent.model.AgentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service
public class BugfixAgent extends BaseAgent {

    private static final Logger logger = LoggerFactory.getLogger(BugfixAgent.class);

    private static final String SYSTEM_PROMPT = """
            You are a debugging and bug-fixing expert. Your role is to:
            - Identify the root cause of bugs and errors
            - Provide clear explanations of what went wrong
            - Suggest specific fixes with code examples
            - Recommend preventive measures to avoid similar issues
            - Consider edge cases and potential side effects
            
            Always explain the reasoning behind your fixes and provide working solutions.
            """;

    public BugfixAgent(ChatModel chatModel) {
        super(chatModel);
    }

    @Override
    public AgentType getType() {
        return AgentType.BUGFIX;
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
        return "🐞 BugfixAgent";
    }

    @Override
    protected String getLogEmoji() {
        return "🔧";
    }
}
