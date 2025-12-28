package com.codingagent.service.agent;

import com.codingagent.service.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class ToolBasedAgent implements Agent {

    private static final Logger logger = LoggerFactory.getLogger(ToolBasedAgent.class);
    private static final int MAX_ITERATIONS = 20;
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
            "TOOL:\\s*(\\w+)\\s*\\{([^}]*)\\}",
            Pattern.DOTALL
    );

    protected final ChatModel chatModel;
    protected final List<Tool> tools;

    protected ToolBasedAgent(ChatModel chatModel, List<Tool> tools) {
        this.chatModel = chatModel;
        this.tools = tools;
    }

    @Override
    public String execute(String prompt, String directoryContext) {
        logger.info("{} starting tool-based execution...", getLogPrefix());
        
        StringBuilder conversationHistory = new StringBuilder();
        conversationHistory.append(buildSystemPrompt()).append("\n\n");
        conversationHistory.append("Available Tools:\n").append(buildToolDescriptions()).append("\n\n");
        
        if (directoryContext != null && !directoryContext.isEmpty()) {
            conversationHistory.append("Directory Context:\n").append(directoryContext).append("\n\n");
        }
        
        conversationHistory.append("User Request: ").append(prompt).append("\n\n");
        conversationHistory.append("Begin your work. Use tools to accomplish the task.\n");

        boolean taskComplete = false;
        int iteration = 0;

        while (!taskComplete && iteration < MAX_ITERATIONS) {
            iteration++;
            logger.info("Iteration {}/{}", iteration, MAX_ITERATIONS);

            Prompt aiPrompt = new Prompt(conversationHistory.toString());
            String response = chatModel.call(aiPrompt).getResult().getOutput().getContent();
            
            logger.info("AI Response:\n{}", truncate(response, 500));
            conversationHistory.append("Assistant: ").append(response).append("\n\n");

            List<ToolCall> toolCalls = extractToolCalls(response);
            
            if (toolCalls.isEmpty()) {
                logger.warn("No tool calls found in response. Prompting agent to use tools.");
                conversationHistory.append("System: You must use tools to complete the task. ")
                        .append("Call tools using format: TOOL: tool_name {parameters}\n\n");
                continue;
            }

            for (ToolCall toolCall : toolCalls) {
                String result = executeTool(toolCall);
                logger.info("Tool {} result: {}", toolCall.toolName, truncate(result, 200));
                
                conversationHistory.append("Tool Result (").append(toolCall.toolName).append("): ")
                        .append(result).append("\n\n");

                if (toolCall.toolName.equals("finish_task")) {
                    taskComplete = true;
                    break;
                }
            }
        }

        if (!taskComplete) {
            logger.warn("Task did not complete within {} iterations", MAX_ITERATIONS);
            conversationHistory.append("\nSystem: Maximum iterations reached. Task incomplete.\n");
        }

        return conversationHistory.toString();
    }

    private String buildToolDescriptions() {
        return tools.stream()
                .map(tool -> String.format("- %s: %s", tool.getName(), tool.getDescription()))
                .collect(Collectors.joining("\n"));
    }

    private List<ToolCall> extractToolCalls(String response) {
        Matcher matcher = TOOL_CALL_PATTERN.matcher(response);
        return matcher.results()
                .map(matchResult -> new ToolCall(
                        matchResult.group(1).trim(),
                        matchResult.group(2).trim()
                ))
                .collect(Collectors.toList());
    }

    private String executeTool(ToolCall toolCall) {
        Tool tool = tools.stream()
                .filter(t -> t.getName().equals(toolCall.toolName))
                .findFirst()
                .orElse(null);

        if (tool == null) {
            return "Error: Unknown tool '" + toolCall.toolName + "'";
        }

        try {
            return tool.execute(toolCall.parameters);
        } catch (Exception e) {
            logger.error("Error executing tool {}", toolCall.toolName, e);
            return "Error: " + e.getMessage();
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    protected abstract String buildSystemPrompt();
    
    protected abstract String getLogPrefix();

    private static class ToolCall {
        final String toolName;
        final String parameters;

        ToolCall(String toolName, String parameters) {
            this.toolName = toolName;
            this.parameters = parameters;
        }
    }
}
