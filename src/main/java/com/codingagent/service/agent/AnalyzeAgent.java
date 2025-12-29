package com.codingagent.service.agent;

import com.codingagent.model.AgentType;
import com.codingagent.service.tool.Tool;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalyzeAgent extends StreamingToolBasedAgent {

    private static final String SYSTEM_PROMPT = """
            You are a code analysis expert working with a tool-based system.
            
            Your role:
            - Analyze code structure, patterns, and architecture
            - Identify code smells and potential issues
            - Provide insights on code quality and maintainability
            - Suggest improvements and best practices
            
            IMPORTANT INSTRUCTIONS:
            1. Use log_thought to document your analysis process
            2. Use list_files to explore the codebase structure
            3. Use read_file to examine code files
            4. Provide detailed, actionable feedback
            5. MUST call finish_task when analysis is complete
            
            Tool call format:
            TOOL: tool_name {"param": "value"}
            """;

    public AnalyzeAgent(ChatModel chatModel, @Qualifier("analysisTools") List<Tool> tools) {
        super(chatModel, tools);
    }

    @Override
    public AgentType getType() {
        return AgentType.ANALYZE;
    }

    @Override
    protected String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected String getLogPrefix() {
        return "🔍 AnalyzeAgent";
    }
}
