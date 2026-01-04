# Agent Collaboration Mode

## Overview

The coding agent now supports **Collaboration Mode** - a sequential pipeline where multiple AI agents work together to produce higher quality code through iterative refinement.

## How It Works

### Standard Mode (Default)
1. Classify request → Select agent
2. Execute agent once
3. Return result

### Collaboration Mode (CODE agent only)
1. **Step 1: Code Generation** - CODE agent generates initial implementation
2. **Step 2: Analysis** - ANALYZE agent reviews the code for:
   - Code quality and best practices
   - Security vulnerabilities
   - Performance issues
   - Potential bugs
   - Design patterns and architecture
3. **Step 3: Refinement** - CODE agent improves the code based on analysis feedback
4. **Result** - Returns refined code + analysis report

## When to Use Collaboration Mode

### ✅ Use Collaboration Mode For:
- **Production code** - Higher quality, more robust implementations
- **Complex features** - Multi-file projects with intricate logic
- **Security-critical code** - Authentication, payment processing, data handling
- **Learning** - See how code can be improved with expert analysis

### ❌ Skip Collaboration Mode For:
- **Simple tasks** - Basic functions, small utilities
- **Quick prototypes** - When speed matters more than quality
- **Analysis/Bugfix requests** - Only CODE agent benefits from collaboration
- **Cost-sensitive scenarios** - Uses 3x AI calls vs standard mode

## Usage

### Via Web UI

1. Open `http://localhost:8081`
2. Enter your coding task
3. ✅ **Check "Enable Collaboration Mode"**
4. Submit request
5. Wait for the 3-step process (takes 2-3x longer)
6. Review the refined code + analysis report

### Via API

```bash
curl -X POST http://localhost:8080/api/agent/process \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Create a REST API for user management",
    "directoryPath": "/path/to/project",
    "useCollaboration": true
  }'
```

### Response Format

```json
{
  "agentType": "CODE",
  "result": "refined code...\n\n--- Analysis Report ---\nanalysis feedback...",
  "reasoning": "Collaborative process: Code Generation → Analysis → Refinement",
  "filesWritten": ["/path/to/file1.java", "/path/to/file2.java"],
  "fileCount": 2
}
```

## Performance Considerations

| Metric | Standard Mode | Collaboration Mode |
|--------|--------------|-------------------|
| **AI Calls** | 1-2 | 3 |
| **Time** | ~30-60s | ~90-180s |
| **Cost** | $ | $$$ |
| **Quality** | Good | Excellent |

## Implementation Details

### Backend Flow

```java
// OrchestratorService.java
public AgentResponse processRequest(String prompt, String directoryPath, Boolean useCollaboration) {
    AgentType selectedType = classifyRequest(prompt);
    
    if (Boolean.TRUE.equals(useCollaboration) && selectedType == AgentType.CODE) {
        return processWithCollaboration(prompt, directoryPath);
    }
    
    // Standard single-agent execution
    return processSingleAgent(selectedType, prompt, directoryPath);
}

private AgentResponse processWithCollaboration(String prompt, String directoryPath) {
    // Step 1: Generate code
    String initialCode = codeAgent.execute(prompt, directoryContext);
    
    // Step 2: Analyze code
    String analysis = analyzeAgent.execute("Analyze this code...", null);
    
    // Step 3: Refine based on analysis
    String refinedCode = codeAgent.execute("Improve based on feedback...", directoryContext);
    
    // Write files and return combined result
    return buildResponse(refinedCode, analysis);
}
```

### Agent Interaction

```
┌─────────────────────────────────────────────────────┐
│              User Request (CODE task)               │
└────────────────────┬────────────────────────────────┘
                     │
                     ▼
         ┌───────────────────────┐
         │   Collaboration Mode? │
         └───────┬───────────────┘
                 │
        ┌────────┴────────┐
        │ Yes             │ No
        ▼                 ▼
┌───────────────┐   ┌─────────────┐
│ CODE Agent    │   │ CODE Agent  │
│ (Generate)    │   │ (Execute)   │
└───────┬───────┘   └──────┬──────┘
        │                  │
        ▼                  ▼
┌───────────────┐      [Return]
│ ANALYZE Agent │
│ (Review)      │
└───────┬───────┘
        │
        ▼
┌───────────────┐
│ CODE Agent    │
│ (Refine)      │
└───────┬───────┘
        │
        ▼
    [Return]
```

## Examples

### Example 1: Simple Function (Standard Mode Recommended)

**Prompt:** "Create a function to calculate factorial"

**Standard Mode:** ✅ Fast, sufficient quality  
**Collaboration Mode:** ❌ Overkill, unnecessary cost

### Example 2: REST API (Collaboration Mode Recommended)

**Prompt:** "Create a Spring Boot REST API for user management with CRUD operations"

**Standard Mode:** ✅ Works, but may miss edge cases  
**Collaboration Mode:** ✅✅ Better error handling, validation, security

**Collaboration Output:**
```
FILE: src/main/java/com/example/UserController.java
[refined code with improvements]

FILE: src/main/java/com/example/UserService.java
[refined code with improvements]

--- Analysis Report ---
✅ Improvements Applied:
- Added input validation with @Valid
- Implemented proper exception handling
- Added security annotations (@PreAuthorize)
- Improved error responses with custom exceptions
- Added pagination support
- Included comprehensive logging
```

## Configuration

Currently enabled by default for all CODE agent requests when the checkbox is checked. No additional configuration needed.

## Future Enhancements

Potential improvements:
- [ ] Configurable iteration count (1-5 refinement cycles)
- [ ] Quality threshold scoring (stop when score > 8/10)
- [ ] Parallel analysis from multiple perspectives
- [ ] Collaboration for BUGFIX agent (analyze → fix → verify)
- [ ] Cost estimation before execution
- [ ] Comparison mode (generate multiple solutions, pick best)

## Troubleshooting

**Issue:** Collaboration mode takes too long  
**Solution:** Use standard mode for simple tasks, or increase timeout settings

**Issue:** Analysis report not showing  
**Solution:** Check that the response includes "--- Analysis Report ---" section

**Issue:** Files not written  
**Solution:** Ensure directory path is provided and CODE agent uses FILE: format

## Cost Optimization Tips

1. **Use selectively** - Reserve for important production code
2. **Test with standard mode first** - Validate the approach before refining
3. **Batch similar requests** - Generate multiple features, then analyze together
4. **Cache analysis** - Reuse analysis patterns for similar code structures
