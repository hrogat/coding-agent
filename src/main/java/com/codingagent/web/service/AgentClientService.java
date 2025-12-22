package com.codingagent.web.service;

import com.codingagent.web.model.AgentRequest;
import com.codingagent.web.model.AgentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class AgentClientService {

    private static final Logger logger = LoggerFactory.getLogger(AgentClientService.class);

    private final WebClient agentWebClient;

    public AgentClientService(WebClient agentWebClient) {
        this.agentWebClient = agentWebClient;
    }

    public Mono<AgentResponse> processRequest(AgentRequest request) {
        logger.info("Sending request to backend: {}", request.getPrompt());

        return agentWebClient.post()
                .uri("/api/agent/process")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AgentResponse.class)
                .doOnSuccess(response -> logger.info("Received response from backend: {}", response.getAgentType()))
                .doOnError(error -> logger.error("Error calling backend", error));
    }
}
