package com.codingagent.service.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CollaborationAgent {

    private static final Logger logger = LoggerFactory.getLogger(CollaborationAgent.class);
    
    private final Agent codeAgent;
    private final Agent analyzeAgent;

    public CollaborationAgent(CodeAgent codeAgent, AnalyzeAgent analyzeAgent) {
        this.codeAgent = codeAgent;
        this.analyzeAgent = analyzeAgent;
    }

    public CollaborationResult executeCollaborativeProcess(String userPrompt, String directoryContext) {
        logger.info("Starting collaborative code generation process");

        logger.info("Step 1/3: Generating initial code");
        String initialCode = codeAgent.execute(userPrompt, directoryContext);

        logger.info("Step 2/3: Analyzing generated code");
        String analysisPrompt = buildAnalysisPrompt(initialCode);
        String analysis = analyzeAgent.execute(analysisPrompt, null);

        logger.info("Step 3/3: Refining code based on analysis");
        String refinementPrompt = buildRefinementPrompt(analysis, initialCode);
        String refinedCode = codeAgent.execute(refinementPrompt, directoryContext);

        String combinedResult = refinedCode + "\n\n--- Analysis Report ---\n" + analysis;

        return new CollaborationResult(refinedCode, analysis, combinedResult);
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

    public static class CollaborationResult {
        private final String refinedCode;
        private final String analysis;
        private final String combinedResult;

        public CollaborationResult(String refinedCode, String analysis, String combinedResult) {
            this.refinedCode = refinedCode;
            this.analysis = analysis;
            this.combinedResult = combinedResult;
        }

        public String getRefinedCode() {
            return refinedCode;
        }

        public String getAnalysis() {
            return analysis;
        }

        public String getCombinedResult() {
            return combinedResult;
        }
    }
}
