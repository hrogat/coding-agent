# Real-Time Chunk Processing Implementation

## Overview
Implemented true real-time streaming where each chunk from `ChatModel.stream()` is processed immediately, tool calls are extracted and executed as soon as they're detected, and events are streamed out instantly.

## How It Works

### 1. Chunk-by-Chunk Processing

**Previous Implementation (WRONG):**
```java
// Collected ALL chunks first, then processed
chatModel.stream(aiPrompt)
    .doOnNext(chatResponse -> {
        response.append(content);
        logger.debug("AI streaming chunk: {}", content);
    })
    .blockLast();

// THEN extracted and executed tools after complete response
List<ToolCall> toolCalls = extractToolCalls(fullResponse);
for (ToolCall toolCall : toolCalls) {
    executeTool(toolCall);
}
```

**New Implementation (CORRECT):**
```java
StringBuilder responseBuffer = new StringBuilder();
Set<String> executedToolCalls = new HashSet<>();

chatModel.stream(aiPrompt)
    .doOnNext(chatResponse -> {
        String chunk = chatResponse.getResult().getOutput().getContent();
        responseBuffer.append(chunk);
        logger.debug("AI chunk: {}", truncate(chunk, 100));
        
        // Extract tool calls from current buffer IMMEDIATELY
        String currentResponse = responseBuffer.toString();
        List<ToolCall> toolCalls = extractToolCalls(currentResponse);
        
        // Execute NEW tool calls IMMEDIATELY
        for (ToolCall toolCall : toolCalls) {
            String toolCallKey = toolCall.toolName + ":" + toolCall.parameters;
            
            if (!executedToolCalls.contains(toolCallKey)) {
                executedToolCalls.add(toolCallKey);
                
                // Execute tool RIGHT NOW
                String result = executeTool(toolCall);
                String displayResult = truncateToolResult(toolCall.toolName, result);
                
                // Stream event RIGHT NOW
                sink.next(StreamEvent.builder()
                    .type(StreamEvent.EventType.TOOL_RESULT)
                    .toolName(toolCall.toolName)
                    .toolResult(displayResult)
                    .message("Tool " + toolCall.toolName + " completed")
                    .build());
                
                // Handle task completion
                if (toolCall.toolName.equals("finish_task")) {
                    taskComplete = true;
                    sink.next(StreamEvent.builder()
                        .type(StreamEvent.EventType.TASK_COMPLETE)
                        .complete(true)
                        .message("Task completed successfully: " + result)
                        .build());
                }
            }
        }
    })
    .blockLast();
```

## Key Features

### 1. Immediate Tool Extraction
- After each chunk arrives, we immediately try to extract tool calls from the accumulated buffer
- No waiting for the complete AI response

### 2. Immediate Tool Execution
- As soon as a tool call is detected, it's executed immediately
- Results are streamed out instantly

### 3. Duplicate Prevention
- Uses `Set<String>` to track executed tool calls
- Key format: `"toolName:parameters"`
- Prevents re-executing the same tool call when subsequent chunks arrive

### 4. Real-Time Event Streaming
- `TOOL_RESULT` events are emitted immediately after tool execution
- `TASK_COMPLETE` event is emitted as soon as `finish_task` is detected
- No batching or buffering of events

## Execution Flow

```
1. Iteration starts → Emit ITERATION_START event
2. Start ChatModel.stream()
3. For each chunk:
   ├─ Append chunk to buffer
   ├─ Log chunk (debug level)
   ├─ Extract tool calls from current buffer
   ├─ For each NEW tool call:
   │  ├─ Check if already executed (skip if yes)
   │  ├─ Mark as executed
   │  ├─ Execute tool immediately
   │  ├─ Log result (debug level)
   │  ├─ Emit TOOL_RESULT event immediately
   │  └─ If finish_task → Emit TASK_COMPLETE immediately
   └─ Continue to next chunk
4. Stream completes
5. Check if tools were found (prompt if not)
6. Next iteration or complete
```

## Benefits

### 1. True Real-Time Streaming
- Events are emitted as they happen, not after completion
- Client sees results immediately

### 2. Lower Latency
- Tool execution starts as soon as detected
- No waiting for complete AI response

### 3. Better User Experience
- Users see progress in real-time
- Faster perceived performance

### 4. Efficient Processing
- Tools can execute while AI is still generating response
- Parallel processing opportunity

## Example Timeline

```
Time | Event
-----|------
0ms  | ITERATION_START emitted
10ms | AI chunk 1: "I will create"
20ms | AI chunk 2: " a file. TOOL: write_file {"
30ms | AI chunk 3: "path": "Hello.java", "content": "..."
40ms | Tool call detected! → Execute write_file immediately
45ms | TOOL_RESULT emitted (file written)
50ms | AI chunk 4: "} Now I will finish. TOOL: finish_task"
60ms | Tool call detected! → Execute finish_task immediately
65ms | TASK_COMPLETE emitted
```

**Previous approach would have:**
```
Time | Event
-----|------
0ms  | ITERATION_START emitted
10ms | AI chunk 1: "I will create"
20ms | AI chunk 2: " a file. TOOL: write_file {"
30ms | AI chunk 3: "path": "Hello.java", "content": "..."
40ms | (waiting...)
50ms | AI chunk 4: "} Now I will finish. TOOL: finish_task"
60ms | AI complete, NOW extract tools
65ms | Execute write_file
70ms | TOOL_RESULT emitted (file written)
75ms | Execute finish_task
80ms | TASK_COMPLETE emitted
```

**Difference: 15ms faster!** (and scales with more tools)

## Logging

### Debug Level
- `AI chunk: {chunk}` - Each chunk as it arrives
- `Executing tool: {name} with parameters: {params}` - When tool starts
- `Tool {name} result: {result}` - When tool completes
- `AI Response complete: {response}` - After stream finishes

### Info Level
- `Iteration {n}/{max}` - Iteration progress

## Edge Cases Handled

### 1. Incomplete Tool Calls
- Tool extraction handles partial JSON gracefully
- Only executes when complete tool call is detected

### 2. Duplicate Detection
- Same tool call won't execute twice
- Uses tool name + parameters as unique key

### 3. Task Completion
- `taskComplete` flag prevents further iterations
- `TASK_COMPLETE` event emitted immediately

### 4. No Tools Found
- After stream completes, checks if any tools were found
- Prompts AI to use tools if none detected

## Testing

```bash
# Start with debug logging to see chunk processing
cd /home/gat/devel/learn/ai-generated/coding-agent
mvn spring-boot:run --debug

# Test streaming
curl -X POST http://localhost:8080/api/agent/stream \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Create a Hello.java file","directoryPath":"/tmp/test"}' \
  --no-buffer
```

**What you'll see:**
- Events stream out as they happen
- Tool results appear immediately after detection
- No waiting for complete AI response

## Performance Impact

### Positive
- ✅ Lower latency for tool execution
- ✅ Better perceived performance
- ✅ True real-time streaming

### Considerations
- Tool extraction runs on each chunk (minimal overhead)
- Duplicate checking uses HashSet (O(1) lookup)
- Overall: Negligible performance impact, significant UX improvement

## Summary

The implementation now provides **true real-time streaming** where:
1. Each AI chunk is processed immediately
2. Tool calls are extracted as soon as they're complete
3. Tools execute immediately upon detection
4. Events stream out instantly
5. No waiting for complete AI response

This is exactly what you wanted: **process each chunk immediately and stream out each defined event in real-time**.
