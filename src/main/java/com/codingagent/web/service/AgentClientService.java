package com.codingagent.web.service;

import com.codingagent.web.model.AgentRequest;
import com.codingagent.web.model.AgentResponse;
import com.codingagent.web.model.StreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AgentClientService {

    private static final Logger logger = LoggerFactory.getLogger(AgentClientService.class);

    private final WebClient webClient;

    public AgentClientService(@Value("${agent.backend.url:http://localhost:8080}") String backendUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(backendUrl)
                .build();
        logger.info("AgentClientService initialized with backend URL: {}", backendUrl);
    }

    public Mono<AgentResponse> processRequest(AgentRequest request) {
        logger.info("Sending request to backend: {}", request.getPrompt());
        
        return webClient.post()
                .uri("/api/agent/process")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AgentResponse.class)
                .doOnSuccess(response -> logger.info("Received response from backend"))
                .doOnError(error -> logger.error("Error calling backend", error));
    }

    public Flux<StreamEvent> processRequestStream(AgentRequest request) {
        logger.info("Sending streaming request to backend: {}", request.getPrompt());
        
        return webClient.post()
                .uri("/api/agent/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(StreamEvent.class)
                .doOnNext(event -> logger.debug("Received event: {}", event.getType()))
                .doOnComplete(() -> logger.info("Stream completed"))
                .doOnError(error -> logger.error("Error in stream", error));
    }
}
