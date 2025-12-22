package com.codingagent.service.agent;

import com.codingagent.model.AgentType;

public interface Agent {
    AgentType getType();
    String execute(String prompt, String directoryContext);
}
