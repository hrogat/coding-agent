package com.codingagent.web.controller;

import com.codingagent.web.model.AgentRequest;
import com.codingagent.web.model.AgentResponse;
import com.codingagent.web.service.AgentClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    private final AgentClientService agentClientService;

    public ApiController(AgentClientService agentClientService) {
        this.agentClientService = agentClientService;
    }

    @PostMapping("/submit")
    public Mono<ResponseEntity<AgentResponse>> submitRequest(@RequestBody AgentRequest request) {
        logger.info("Received web request: {}", request.getPrompt());

        return agentClientService.processRequest(request)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    logger.error("Error processing request", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }
}
