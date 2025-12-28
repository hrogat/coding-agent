package com.codingagent.service.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogThoughtTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(LogThoughtTool.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
        try {
            JsonNode jsonNode = objectMapper.readTree(parameters.trim());
            return jsonNode.has("thought") ? jsonNode.get("thought").asText() : parameters.trim();
        } catch (Exception e) {
            logger.debug("Failed to parse as JSON, using raw parameter: {}", parameters);
            return parameters.trim();
        }
    }
}
