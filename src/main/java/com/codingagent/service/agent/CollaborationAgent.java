package com.codingagent.service.agent;

import com.codingagent.model.StreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicReference;

@Service
public class CollaborationAgent {

    private static final Logger logger = LoggerFactory.getLogger(CollaborationAgent.class);
    
    private final CodeAgent codeAgent;
    private final AnalyzeAgent analyzeAgent;

    public CollaborationAgent(CodeAgent codeAgent, AnalyzeAgent analyzeAgent) {
        this.codeAgent = codeAgent;
        this.analyzeAgent = analyzeAgent;
    }

    public Flux<StreamEvent> executeCollaborativeStream(String userPrompt, String directoryContext, String baseDirectory) {
        logger.info("Starting collaborative code generation process (streaming)");

        AtomicReference<String> initialCodeRef = new AtomicReference<>("");
        AtomicReference<String> analysisRef = new AtomicReference<>("");

        return Flux.concat(
            emitPhaseEvent("Step 1/3: Generating initial code"),
            codeAgent.executeStream(userPrompt, directoryContext, baseDirectory)
                .doOnNext(event -> {
                    if (event.getMessage() != null) {
                        initialCodeRef.updateAndGet(current -> current + event.getMessage() + "\n");
                    }
                }),
            
            emitPhaseEvent("Step 2/3: Analyzing generated code"),
            Flux.defer(() -> {
                String analysisPrompt = buildAnalysisPrompt(initialCodeRef.get());
                return analyzeAgent.executeStream(analysisPrompt, null, baseDirectory)
                    .doOnNext(event -> {
                        if (event.getMessage() != null) {
                            analysisRef.updateAndGet(current -> current + event.getMessage() + "\n");
                        }
                    });
            }),
            
            emitPhaseEvent("Step 3/3: Refining code based on analysis"),
            Flux.defer(() -> {
                String refinementPrompt = buildRefinementPrompt(analysisRef.get(), initialCodeRef.get());
                return codeAgent.executeStream(refinementPrompt, directoryContext, baseDirectory);
            }),
            
            emitPhaseEvent("Collaboration complete")
        );
    }

    private Flux<StreamEvent> emitPhaseEvent(String message) {
        return Flux.just(StreamEvent.builder()
            .type(StreamEvent.EventType.LOG)
            .message("🤝 CollaborationAgent: " + message)
            .build());
    }

    private String buildAnalysisPrompt(String code) {
        return String.format("""
            Analyze the following code for:
            - Code quality and best practices
            - Security vulnerabilities
            - Performance issues
            - Potential bugs
            - Design patterns and architecture
            
            Provide specific, actionable feedback for improvements.
            
            Code to analyze:
            %s
            """, code);
    }

    private String buildRefinementPrompt(String analysis, String originalCode) {
        return String.format("""
            Improve the following code based on this analysis feedback.
            Apply all suggested improvements and maintain the FILE: format for file operations.
            
            Analysis feedback:
            %s
            
            Original code:
            %s
            
            Provide the improved code with all files in the FILE: format.
            """, analysis, originalCode);
    }

}
