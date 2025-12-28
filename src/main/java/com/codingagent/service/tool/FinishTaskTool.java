package com.codingagent.service.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FinishTaskTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(FinishTaskTool.class);

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
        return "TASK_COMPLETE: " + summary;
    }

    private String extractSummary(String parameters) {
        String cleaned = parameters.trim();
        if (cleaned.startsWith("{") && cleaned.contains("\"summary\"")) {
            int start = cleaned.indexOf("\"summary\"");
            int colonIdx = cleaned.indexOf(":", start);
            int valueStart = cleaned.indexOf("\"", colonIdx) + 1;
            int valueEnd = cleaned.lastIndexOf("\"");
            return cleaned.substring(valueStart, valueEnd);
        }
        return cleaned;
    }
}
