package com.codingagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private int maxPromptLength = 10000;
    private int streamingLineMaxLength = 120;

    public int getMaxPromptLength() {
        return maxPromptLength;
    }

    public void setMaxPromptLength(int maxPromptLength) {
        this.maxPromptLength = maxPromptLength;
    }

    public int getStreamingLineMaxLength() {
        return streamingLineMaxLength;
    }

    public void setStreamingLineMaxLength(int streamingLineMaxLength) {
        this.streamingLineMaxLength = streamingLineMaxLength;
    }
}
