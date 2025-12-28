package com.codingagent.service.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogThoughtTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(LogThoughtTool.class);

    @Override
    public String getName() {
        return "log_thought";
    }

    @Override
    public String getDescription() {
        return """
                Logs a thought or reasoning step. Use this to explain your thinking process.
                Parameters: {"thought": "your reasoning here"}
                Returns: Confirmation message.
                """;
    }

    @Override
    public String execute(String parameters) {
        String thought = extractThought(parameters);
        logger.info("💭 Agent thought: {}", thought);
        return "Thought logged: " + thought;
    }

    private String extractThought(String parameters) {
        String cleaned = parameters.trim();
        if (cleaned.startsWith("{") && cleaned.contains("\"thought\"")) {
            int start = cleaned.indexOf("\"thought\"");
            int colonIdx = cleaned.indexOf(":", start);
            int valueStart = cleaned.indexOf("\"", colonIdx) + 1;
            int valueEnd = cleaned.lastIndexOf("\"");
            return cleaned.substring(valueStart, valueEnd);
        }
        return cleaned;
    }
}
