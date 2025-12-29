package com.codingagent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamEvent {
    private EventType type;
    private String message;
    private String toolName;
    private String toolParameters;
    private String toolResult;
    private Integer iteration;
    private Boolean complete;
    private String error;

    public enum EventType {
        ITERATION_START,
        AI_THINKING,
        AI_RESPONSE,
        TOOL_CALL,
        TOOL_RESULT,
        TASK_COMPLETE,
        ERROR,
        LOG
    }
}
