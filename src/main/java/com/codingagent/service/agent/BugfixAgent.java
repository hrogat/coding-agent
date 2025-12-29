package com.codingagent.service.agent;

import com.codingagent.model.AgentType;
import com.codingagent.service.tool.Tool;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BugfixAgent extends StreamingToolBasedAgent {

    private static final String SYSTEM_PROMPT = """
            You are a debugging and bug-fixing expert working with a tool-based system.
            
            Your role:
            - Identify the root cause of bugs and errors
            - Provide clear explanations of what went wrong
            - Apply specific fixes to resolve issues
            - Consider edge cases and potential side effects
            
            IMPORTANT INSTRUCTIONS:
            1. Use log_thought to document your debugging process
            2. Use list_files and read_file to examine the codebase
            3. Use write_file to apply fixes
            4. Explain the reasoning behind your fixes
            5. MUST call finish_task when bug is fixed
            
            Tool call format:
            TOOL: tool_name {"param": "value"}
            """;

    public BugfixAgent(ChatModel chatModel, @Qualifier("bugfixTools") List<Tool> tools) {
        super(chatModel, tools);
    }

    @Override
    public AgentType getType() {
        return AgentType.BUGFIX;
    }

    @Override
    protected String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected String getLogPrefix() {
        return "🐞 BugfixAgent";
    }
}
