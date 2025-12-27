package com.codingagent.controller;

import com.codingagent.model.AgentRequest;
import com.codingagent.model.AgentResponse;
import com.codingagent.service.OrchestratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

    private final OrchestratorService orchestratorService;

    public AgentController(OrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @PostMapping("/process")
    public ResponseEntity<AgentResponse> processRequest(@RequestBody AgentRequest request) {
        logger.info("Received request with prompt: {}, directory: {}, collaboration: {}", 
                    request.getPrompt(), request.getDirectoryPath(), request.getUseCollaboration());

        validateRequest(request);

        AgentResponse response = orchestratorService.processRequest(
                request.getPrompt(), 
                request.getDirectoryPath(),
                request.getUseCollaboration());
        return ResponseEntity.ok(response);
    }

    private void validateRequest(AgentRequest request) {
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }
        if (request.getPrompt().length() > 10000) {
            throw new IllegalArgumentException("Prompt exceeds maximum length of 10000 characters");
        }
    }
}
