package com.codingagent.service.agent;

import com.codingagent.model.StreamEvent;
import com.codingagent.service.tool.Tool;
import com.codingagent.service.tool.ToolExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class StreamingToolBasedAgent implements Agent {

    private static final Logger logger = LoggerFactory.getLogger(StreamingToolBasedAgent.class);
    private static final int MAX_ITERATIONS = 20;
    private static final int MAX_FILE_CONTENT_LOG_LENGTH = 200;
    private static final int MAX_DISPLAY_CONTENT_LENGTH = 500;
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
            "TOOL:\s*(\w+)\s*(\{.*?\}(?=\s*(?:TOOL:|Assistant:|$)))",
            Pattern.DOTALL
    );

    protected final ChatModel chatModel;
    protected final List<Tool> tools;

    protected StreamingToolBasedAgent(ChatModel chatModel, List<Tool> tools) {
        this.chatModel = chatModel;
        this.tools = tools;
    }

    @Override
    public String execute(String prompt, String directoryContext) {
        StringBuilder result = new StringBuilder();
        executeStream(prompt, directoryContext, null).toStream().forEach(event -> {
            if (event.getMessage() != null) {
                // Append new line for all events, including TASK_COMPLETE
                result.append(event.getMessage()).append("\n");
            }
        });
        return result.toString();
    }

    public Flux<StreamEvent> executeStream(String prompt, String directoryContext, String baseDirectory) {
        return Flux.create(sink -> {
            try {
                // Set base directory in thread-local context for tools to use
                if (baseDirectory != null && !baseDirectory.trim().isEmpty()) {
                    ToolExecutionContext.setBaseDirectory(baseDirectory);
                }
                executeWithSinkAsync(prompt, directoryContext, sink);
            } catch (Exception e) {
                logger.error("Error during streaming execution", e);
                sink.next(StreamEvent.builder()
                        .type(StreamEvent.EventType.ERROR)
                        .error(e.getMessage())
                        .message("Fatal error: " + e.getMessage())
                        .build());
                sink.complete();
            } finally {
                // Clean up thread-local context
                ToolExecutionContext.clear();
            }
        });
    }

    private void executeWithSinkAsync(String prompt, String directoryContext, reactor.core.publisher.FluxSink<StreamEvent> sink) {
        logger.info("{} starting streaming tool-based execution...", getLogPrefix());

        StringBuilder conversationHistory = new StringBuilder();
        conversationHistory.append(buildSystemPrompt()).append("\n\n");
        conversationHistory.append("Available Tools:\n").append(buildToolDescriptions()).append("\n\n");

        if (directoryContext != null && !directoryContext.isEmpty()) {
            conversationHistory.append("Directory Context:\n").append(directoryContext).append("\n\n");
        }

        conversationHistory.append("User Request: ").append(prompt).append("\n\n");
        conversationHistory.append("Begin your work. Use tools to accomplish the task.\n");

        AtomicBoolean taskComplete = new AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicInteger iteration = new java.util.concurrent.atomic.AtomicInteger(0);

        processNextIteration(conversationHistory, taskComplete, iteration, sink);
    }

    private void processNextIteration(StringBuilder conversationHistory, AtomicBoolean taskComplete, 
                                     java.util.concurrent.atomic.AtomicInteger iteration, 
                                     reactor.core.publisher.FluxSink<StreamEvent> sink) {
        if (taskComplete.get() || iteration.get() >= MAX_ITERATIONS) {
            if (!taskComplete.get()) {
                logger.warn("Task did not complete within {} iterations", MAX_ITERATIONS);
                sink.next(StreamEvent.builder()
                        .type(StreamEvent.EventType.ERROR)
                        .error("Maximum iterations reached")
                        .message("Task incomplete: Maximum iterations (" + MAX_ITERATIONS + ") reached")
                        .build());
            }
            sink.complete();
            return;
        }

        iteration.incrementAndGet();
        
        sink.next(StreamEvent.builder()
                .type(StreamEvent.EventType.ITERATION_START)
                .iteration(iteration.get())
                .message("Starting iteration " + iteration.get() + " of " + MAX_ITERATIONS)
                .build());

        logger.info("Iteration {}/{}", iteration.get(), MAX_ITERATIONS);

        Prompt aiPrompt = new Prompt(conversationHistory.toString());
        StringBuilder responseBuffer = new StringBuilder();
        java.util.Set<String> executedToolCalls = new java.util.HashSet<>();
        
        // Process AI response chunks in real-time without blocking
        chatModel.stream(aiPrompt)
                .doOnNext(chatResponse -> {
                    String chunk = chatResponse.getResult().getOutput().getContent();
                    responseBuffer.append(chunk);
                    logger.debug("AI chunk: {}", truncate(chunk, 100));
                    
                    // Try to extract and execute tool calls as chunks arrive
                    String currentResponse = responseBuffer.toString();
                    List<ToolCall> toolCalls = extractToolCalls(currentResponse);
                    
                    // Process only new tool calls (not already executed)
                    for (ToolCall toolCall : toolCalls) {
                        String toolCallKey = toolCall.toolName + ":" + toolCall.parameters;
                        
                        if (!executedToolCalls.contains(toolCallKey)) {
                            executedToolCalls.add(toolCallKey);
                            
                            logger.debug("Executing tool: {} with parameters: {}", 
                                    toolCall.toolName, truncate(toolCall.parameters, 200));
                            
                            String result = executeTool(toolCall);
                            String displayResult = truncateToolResult(toolCall.toolName, result);
                            
                            logger.debug("Tool {} result: {}", toolCall.toolName,
                                    truncate(displayResult, MAX_FILE_CONTENT_LOG_LENGTH));
                            
                            // Stream tool result event immediately
                            sink.next(StreamEvent.builder()
                                    .type(StreamEvent.EventType.TOOL_RESULT)
                                    .toolName(toolCall.toolName)
                                    .toolResult(displayResult)
                                    .message("Tool " + toolCall.toolName + " completed")
                                    .build());
                            
                            conversationHistory.append("Tool Result (")
                                    .append(toolCall.toolName).append("): ")
                                    .append(result).append("\n\n");
                            
                            if (toolCall.toolName.equals("finish_task")) {
                                taskComplete.set(true);
                                // Extract the summary from the result
                                String summary = result.replace("TASK_COMPLETE: ", "");
                                // Ensure the summary ends with a new line for proper formatting
                                if (!summary.endsWith("\n")) {
                                    summary = summary + "\n";
                                }
                                sink.next(StreamEvent.builder()
                                        .type(StreamEvent.EventType.TASK_COMPLETE)
                                        .complete(true)
                                        .message("Task completed successfully:\n\n" + summary)
                                        .build());
                            }
                        }
                    }
                })
                .doOnComplete(() -> {
                    String fullResponse = responseBuffer.toString();
                    logger.debug("AI Response complete: {}", truncate(fullResponse, 500));
                    conversationHistory.append("Assistant: ").append(fullResponse).append("\n\n");
                    
                    // Check if we need to prompt for tools
                    List<ToolCall> finalToolCalls = extractToolCalls(fullResponse);
                    if (finalToolCalls.isEmpty() && !taskComplete.get()) {
                        logger.warn("No tool calls found in response. Prompting agent to use tools.");
                        conversationHistory.append("System: You must use tools to complete the task. ")
                                .append("Call tools using format: TOOL: tool_name {parameters}\n\n");
                    }
                    
                    // Process next iteration recursively
                    processNextIteration(conversationHistory, taskComplete, iteration, sink);
                })
                .doOnError(error -> {
                    logger.error("Error during AI streaming", error);
                    sink.next(StreamEvent.builder()
                            .type(StreamEvent.EventType.ERROR)
                            .error(error.getMessage())
                            .message("Error during AI processing: " + error.getMessage())
                            .build());
                    sink.complete();
                })
                .subscribe();
    }

    private String buildToolDescriptions() {
        return tools.stream()
                .map(tool -> String.format("- %s: %s", tool.getName(), tool.getDescription()))
                .collect(Collectors.joining("\n"));
    }

    private List<ToolCall> extractToolCalls(String response) {
        Matcher matcher = TOOL_CALL_PATTERN.matcher(response);
        return matcher.results()
                .map(matchResult -> {
                    String toolName = matchResult.group(1).trim();
                    String params = matchResult.group(2).trim();
                    
                    // Validate that JSON is complete before returning
                    if (isCompleteJson(params)) {
                        return new ToolCall(toolName, params);
                    }
                    return null;
                })
                .filter(toolCall -> toolCall != null)
                .collect(Collectors.toList());
    }
    
    private boolean isCompleteJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        
        // Simple validation: count braces and check for complete structure
        int braceCount = 0;
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                }
            }
        }
        
        // JSON is complete if all braces are balanced and we're not in a string
        return braceCount == 0 && !inString;
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
            logger.debug("Executing tool {} with parameters: {}", toolCall.toolName, 
                    truncate(toolCall.parameters, 200));
            return tool.execute(toolCall.parameters);
        } catch (Exception e) {
            logger.error("Error executing tool {}", toolCall.toolName, e);
            return "Error: " + e.getMessage();
        }
    }

    private String truncateToolParameters(String toolName, String parameters) {
        if (toolName.equals("write_file")) {
            try {
                if (parameters.contains("\"content\"")) {
                    int contentStart = parameters.indexOf("\"content\"");
                    int valueStart = parameters.indexOf(":", contentStart) + 1;
                    int valueEnd = findJsonStringEnd(parameters, valueStart);
                    
                    if (valueEnd > valueStart) {
                        String beforeContent = parameters.substring(0, valueStart);
                        String afterContent = parameters.substring(valueEnd);
                        return beforeContent + " \"[CONTENT TRUNCATED]\"" + afterContent;
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not truncate content in parameters", e);
            }
        }
        return truncate(parameters, MAX_DISPLAY_CONTENT_LENGTH);
    }

    private String truncateToolResult(String toolName, String result) {
        if (toolName.equals("read_file")) {
            // For read_file, only show filename, not content
            if (result != null && result.startsWith("Error")) {
                return result;
            }
            return "[File read successfully - content not displayed]";
        } else if (toolName.equals("write_file")) {
            // For write_file, show success message (no content)
            return result;
        } else if (toolName.equals("list_files")) {
            // For list_files, truncate if too long
            return truncate(result, MAX_DISPLAY_CONTENT_LENGTH);
        } else {
            // For other tools (log_thought, finish_task), show complete
            return result;
        }
    }

    private int findJsonStringEnd(String json, int start) {
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                if (!inString) {
                    inString = true;
                } else {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... ";
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
