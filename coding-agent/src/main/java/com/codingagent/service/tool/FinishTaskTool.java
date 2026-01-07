package com.codingagent.service.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FinishTaskTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(FinishTaskTool.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "finish_task";
    }

    @Override
    public String getDescription() {
        return """
                Signals that the task is complete. MUST be called when all work is done.
                Parameters: {"summary": "brief summary of what was accomplished"}
                Returns: Task completion confirmation.
                """;
    }

    @Override
    public String execute(String parameters) {
        String summary = extractSummary(parameters);
        logger.info("✅ Task completed: {}", summary);
        
        // Return a structured JSON response for better formatting
        ObjectNode response = objectMapper.createObjectNode();
        response.put("status", "TASK_COMPLETE");
        response.put("summary", summary);
        response.put("timestamp", System.currentTimeMillis());
        
        return response.toString();
    }

    private String extractSummary(String parameters) {
        try {
            JsonNode jsonNode = objectMapper.readTree(parameters.trim());
            return jsonNode.has("summary") ? jsonNode.get("summary").asText() : parameters.trim();
        } catch (Exception e) {
            logger.debug("Failed to parse as JSON, using raw parameter: {}", parameters);
            return parameters.trim();
        }
    }
}