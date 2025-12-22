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

        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            AgentResponse response = orchestratorService.processRequest(
                    request.getPrompt(), 
                    request.getDirectoryPath(),
                    request.getUseCollaboration());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing request", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
