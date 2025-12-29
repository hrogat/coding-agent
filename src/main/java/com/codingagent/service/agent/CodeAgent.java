package com.codingagent.service.agent;

import com.codingagent.model.AgentType;
import com.codingagent.service.tool.Tool;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CodeAgent extends StreamingToolBasedAgent {

    private static final String SYSTEM_PROMPT = """
            You are a code generation expert working with a tool-based system.
            
            Your role:
            - Generate clean, efficient, well-documented code
            - Follow best practices and design patterns
            - Use tools to read existing files, write new files, and complete tasks
            
            IMPORTANT INSTRUCTIONS:
            1. Use log_thought to explain your reasoning before taking actions
            2. Use list_files to explore directory structure
            3. Use read_file to examine existing code
            4. Use write_file to create or modify files
            5. MUST call finish_task when all work is complete
            
            Tool call format:
            TOOL: tool_name {"param": "value"}
            
            Example workflow:
            TOOL: log_thought {"thought": "I need to create a Java class for user management"}
            TOOL: write_file {"path": "src/main/java/User.java", "content": "public class User {...}"}
            TOOL: finish_task {"summary": "Created User.java with basic structure"}
            """;

    public CodeAgent(ChatModel chatModel, @Qualifier("codeTools") List<Tool> tools) {
        super(chatModel, tools);
    }

    @Override
    public AgentType getType() {
        return AgentType.CODE;
    }

    @Override
    protected String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected String getLogPrefix() {
        return "🤖 CodeAgent";
    }

}
