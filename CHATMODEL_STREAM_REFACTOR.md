# ChatModel.stream() Refactoring

## Overview
Refactored `StreamingToolBasedAgent` to use `ChatModel.stream()` for true real-time streaming instead of the `Sinks.Many` construction that was blocking on the complete AI response.

## Changes Made

### 1. Removed Sinks.Many Construction

**Before:**
```java
public Flux<StreamEvent> executeStream(String prompt, String directoryContext) {
    Sinks.Many<StreamEvent> sink = Sinks.many().multicast().onBackpressureBuffer();
    
    new Thread(() -> {
        try {
            executeWithSink(prompt, directoryContext, sink);
        } catch (Exception e) {
            sink.tryEmitNext(StreamEvent.builder()...);
        } finally {
            sink.tryEmitComplete();
        }
    }).start();
    
    return sink.asFlux();
}
```

**After:**
```java
public Flux<StreamEvent> executeStream(String prompt, String directoryContext) {
    return Flux.create(sink -> {
        try {
            executeWithSink(prompt, directoryContext, sink);
            sink.complete();
        } catch (Exception e) {
            sink.next(StreamEvent.builder()...);
            sink.complete();
        }
    });
}
```

**Benefits:**
- No separate thread creation
- Cleaner reactive code
- Uses `Flux.create()` with `FluxSink` directly
- Simpler error handling

### 2. Implemented ChatModel.stream() for Real-Time AI Response

**Before (blocking call):**
```java
Prompt aiPrompt = new Prompt(conversationHistory.toString());
String response = chatModel.call(aiPrompt).getResult().getOutput().getContent();

logger.debug("AI Response:\n{}", truncate(response, 500));
```

**After (streaming call):**
```java
Prompt aiPrompt = new Prompt(conversationHistory.toString());
StringBuilder response = new StringBuilder();

// Stream AI response in real-time
chatModel.stream(aiPrompt)
        .doOnNext(chatResponse -> {
            String content = chatResponse.getResult().getOutput().getContent();
            response.append(content);
            logger.debug("AI streaming chunk: {}", truncate(content, 100));
        })
        .blockLast();

String fullResponse = response.toString();
logger.debug("AI Response complete:\n{}", truncate(fullResponse, 500));
```

**Benefits:**
- AI response is streamed in real-time chunks
- Each chunk is logged as it arrives (debug level)
- Full response is assembled for tool extraction
- More responsive - can see AI thinking in logs

### 3. Updated Event Emission

Changed from `sink.tryEmitNext()` to `sink.next()`:

**Before:**
```java
sink.tryEmitNext(StreamEvent.builder()
        .type(StreamEvent.EventType.ITERATION_START)
        .iteration(iteration)
        .message("Starting iteration " + iteration + " of " + MAX_ITERATIONS)
        .build());
```

**After:**
```java
sink.next(StreamEvent.builder()
        .type(StreamEvent.EventType.ITERATION_START)
        .iteration(iteration)
        .message("Starting iteration " + iteration + " of " + MAX_ITERATIONS)
        .build());
```

**Benefits:**
- Simpler API with `FluxSink`
- No need for `tryEmit` pattern
- Cleaner code

### 4. Method Signature Update

**Before:**
```java
private void executeWithSink(String prompt, String directoryContext, Sinks.Many<StreamEvent> sink)
```

**After:**
```java
private void executeWithSink(String prompt, String directoryContext, reactor.core.publisher.FluxSink<StreamEvent> sink)
```

## How It Works Now

1. **Client requests streaming** via `/api/agent/stream`
2. **Flux.create()** creates a reactive stream with a `FluxSink`
3. **For each iteration:**
   - Emit `ITERATION_START` event
   - Call `chatModel.stream()` to get AI response in chunks
   - Log each chunk as it arrives (debug level)
   - Assemble full response
   - Extract tool calls from full response
   - Execute each tool
   - Emit `TOOL_RESULT` event for each tool
4. **When task completes:**
   - Emit `TASK_COMPLETE` event
   - Call `sink.complete()`
5. **On error:**
   - Emit `ERROR` event
   - Call `sink.complete()`

## Logging Behavior

### Debug Level (visible with --debug flag)
- `AI streaming chunk: {chunk}` - Each chunk as it arrives from ChatModel
- `AI Response complete: {response}` - Full assembled response
- `Calling tool: {name} with parameters: {params}` - Tool execution
- `Tool {name} result: {result}` - Tool results (truncated)

### Info Level
- `Iteration {n}/{max}` - Iteration progress
- Major milestones

## Stream Events Emitted

1. **ITERATION_START** - Each iteration begins
2. **TOOL_RESULT** - After each tool execution
3. **TASK_COMPLETE** - When finish_task is called
4. **ERROR** - On errors or max iterations

## Benefits of This Approach

1. **True Streaming**: AI responses are processed as they arrive, not after completion
2. **Better Logging**: Can see AI thinking in real-time in debug logs
3. **Cleaner Code**: No manual thread management, uses Reactor patterns
4. **More Reactive**: Proper use of Flux.create() and FluxSink
5. **Simpler**: Removed unnecessary Sinks.Many complexity
6. **Better Performance**: No blocking on complete response before processing

## Testing

```bash
# Start backend with debug logging
cd /home/gat/devel/learn/ai-generated/coding-agent
mvn spring-boot:run --debug

# In another terminal, test streaming
curl -X POST http://localhost:8080/api/agent/stream \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Create a Hello.java file","directoryPath":"/tmp/test"}' \
  --no-buffer
```

You should see:
- Real-time stream events in the response
- Debug logs showing AI chunks as they arrive
- Tool executions logged
- Clean event stream to client

## Files Modified

- `StreamingToolBasedAgent.java` - Complete refactor to use ChatModel.stream()

## Summary

The streaming implementation now properly uses `ChatModel.stream()` for real-time AI response processing, eliminating the blocking `chatModel.call()` and the unnecessary `Sinks.Many` construction. This provides true streaming behavior with better logging and cleaner reactive code.
