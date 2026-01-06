package com.codingagent.web.controller;

import com.codingagent.web.model.AgentRequest;
import com.codingagent.web.model.AgentResponse;
import com.codingagent.web.model.StreamEvent;
import com.codingagent.web.service.AgentClientService;
import com.codingagent.web.service.PotentiationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    private final AgentClientService agentClientService;
    private final PotentiationService potentiationService;

    public ApiController(AgentClientService agentClientService, PotentiationService potentiationService) {
        this.agentClientService = agentClientService;
        this.potentiationService = potentiationService;
    }

    @PostMapping("/submit")
    public Mono<ResponseEntity<AgentResponse>> submitRequest(@RequestBody AgentRequest request) {
        logger.info("Received request: {}", request.getPrompt());
        
        return agentClientService.processRequest(request)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    logger.error("Error processing request", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<StreamEvent> streamRequest(@RequestBody AgentRequest request) {
        logger.info("Received streaming request: {}", request.getPrompt());
        
        return agentClientService.processRequestStream(request);
    }

    @GetMapping("/potentiation")
    public ResponseEntity<Double> calculatePotentiation(@RequestParam double base, @RequestParam double exponent) {
        logger.info("Calculating potentiation: {}^ {}", base, exponent);
        
        double result = potentiationService.calculatePotentiation(base, exponent);
        return ResponseEntity.ok(result);
    }
}