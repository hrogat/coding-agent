package com.codingagent.web;

import com.codingagent.web.config.CodingAgentWebProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CodingAgentWebProperties.class)
public class CodingAgentWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodingAgentWebApplication.class, args);
    }
}
