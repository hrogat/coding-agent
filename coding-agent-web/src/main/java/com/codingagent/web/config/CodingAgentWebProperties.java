package com.codingagent.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "coding-agent")
public record CodingAgentWebProperties(
        String backendUrl,
        int timeoutMinutes,
        String defaultDirectory
) {

    public String resolvedDefaultDirectory() {
        if (StringUtils.hasText(defaultDirectory)) {
            return defaultDirectory;
        }
        return System.getProperty("user.dir");
    }
}
