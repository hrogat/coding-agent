package com.codingagent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {
    private AgentType agentType;
    private String result;
    private String reasoning;
    private List<String> filesWritten;
    private Integer fileCount;
}
