# Streaming Implementation Guide

## Overview
The coding-agent application now supports **real-time streaming** of AI operations, tool executions, and results via Server-Sent Events (SSE).

## Backend Architecture (coding-agent)

### 1. StreamEvent Model
**Location**: `src/main/java/com/codingagent/model/StreamEvent.java`

Event types:
- `ITERATION_START` - New iteration begins
- `AI_THINKING` - AI is processing
- `AI_RESPONSE` - AI's reasoning/response
- `TOOL_CALL` - Tool is being executed
- `TOOL_RESULT` - Tool execution result
- `TASK_COMPLETE` - Task finished successfully
- `ERROR` - Error occurred
- `LOG` - General log message

### 2. StreamingToolBasedAgent
**Location**: `src/main/java/com/codingagent/service/agent/StreamingToolBasedAgent.java`

**Key Features**:
- Emits events in real-time during execution
- **File content truncation**:
  - `write_file` parameters: Content replaced with `[CONTENT TRUNCATED]`
  - `read_file` results: Shows `[File content not displayed in stream]`
  - Log outputs: File contents truncated to 200 characters
- Uses Reactor `Flux<StreamEvent>` for streaming
- Thread-safe event emission with `Sinks.Many`

**Methods**:
- `executeStream(prompt, context)` - Returns `Flux<StreamEvent>`
- `execute(prompt, context)` - Non-streaming fallback

### 3. Updated Agents
All agents now extend `StreamingToolBasedAgent`:
- `CodeAgent` - Code generation with streaming
- `BugfixAgent` - Bug fixing with streaming
- `AnalyzeAgent` - Code analysis with streaming

### 4. Controller Endpoints
**Location**: `src/main/java/com/codingagent/controller/AgentController.java`

**Endpoints**:
- `POST /api/agent/process` - Non-streaming (returns `AgentResponse`)
- `POST /api/agent/stream` - **Streaming SSE** (returns `Flux<StreamEvent>`)

**Request Body**:
```json
{
  "prompt": "Create a REST API",
  "directoryPath": "/path/to/project",
  "useCollaboration": false
}
```

### 5. OrchestratorService
**Location**: `src/main/java/com/codingagent/service/OrchestratorService.java`

**New Method**:
```java
public Flux<StreamEvent> processRequestStream(
    String userPrompt, 
    String directoryPath, 
    Boolean useCollaboration
)
```

Automatically detects if agent supports streaming and delegates accordingly.

## Frontend Architecture (coding-agent-web)

### 1. StreamEvent Model
**Location**: `src/main/java/com/codingagent/web/model/StreamEvent.java`

Mirrors backend StreamEvent structure.

### 2. AgentClientService
**Location**: `src/main/java/com/codingagent/web/service/AgentClientService.java`

**Methods**:
- `processRequest(request)` - Non-streaming
- `processRequestStream(request)` - **Streaming** (returns `Flux<StreamEvent>`)

Uses Spring WebClient with `TEXT_EVENT_STREAM` media type.

### 3. ApiController
**Location**: `src/main/java/com/codingagent/web/controller/ApiController.java`

**Endpoints**:
- `POST /api/submit` - Non-streaming
- `POST /api/stream` - **Streaming SSE** (proxies from backend)

### 4. Frontend UI

#### JavaScript
**Location**: `src/main/resources/static/js/app-streaming.js`

**Features**:
- Consumes SSE stream using Fetch API
- Parses `data:` lines from event stream
- Real-time event display
- Auto-scrolling to latest events
- Content truncation for display (500 chars)

**Event Handling**:
```javascript
displayEvent(event) {
  switch(event.type) {
    case 'ITERATION_START': // Purple gradient header
    case 'AI_THINKING': // Orange thinking indicator
    case 'AI_RESPONSE': // Blue AI response card
    case 'TOOL_CALL': // Orange tool call card
    case 'TOOL_RESULT': // Green result card
    case 'TASK_COMPLETE': // Green success banner
    case 'ERROR': // Red error card
  }
}
```

#### CSS
**Location**: `src/main/resources/static/css/streaming.css`

**Styling**:
- Color-coded event cards
- Smooth slide-in animations
- Scrollable container with custom scrollbar
- Responsive design
- Gradient headers for key events

#### HTML
**Location**: `src/main/resources/templates/index.html`

**UI Elements**:
- Task description textarea
- Directory path input
- Collaboration mode checkbox
- Submit button
- Live streaming result display

## What the Client Sees

### 1. Iteration Progress
```
🔄 Iteration 1/20
Starting iteration 1 of 20
```

### 2. AI Thinking
```
🤔 AI Thinking
AI is thinking...
```

### 3. AI Response
```
🤖 AI Response
I will create a REST API with the following endpoints...
```

### 4. Tool Calls
```
🔧 Tool Call: write_file
Calling tool: write_file
Parameters: {"path": "UserController.java", "content": "[CONTENT TRUNCATED]"}
```

### 5. Tool Results
```
✅ Tool Result: write_file
Success: File written to UserController.java
```

### 6. Read Operations
```
✅ Tool Result: read_file
[File content not displayed in stream]
```

### 7. Task Completion
```
✅ Task Complete
Task completed successfully: All files created successfully
```

### 8. Errors
```
❌ Error
Error: File path cannot be empty
```

## Content Truncation Rules

### Backend Logs
- **write_file**: Content truncated to 200 characters
- **read_file**: Content truncated to 200 characters
- **Tool parameters**: Truncated to 200 characters in debug logs

### Frontend Display
- **write_file parameters**: Shows `[CONTENT TRUNCATED]` instead of full content
- **read_file results**: Shows `[File content not displayed in stream]`
- **Other results**: Truncated to 500 characters
- **AI responses**: Full text displayed with line breaks

## Testing

### Start Backend
```bash
cd /home/gat/devel/learn/ai-generated/coding-agent
mvn spring-boot:run
```
Backend runs on: `http://localhost:8080`

### Start Frontend
```bash
cd /home/gat/devel/learn/ai-generated/coding-agent-web
mvn spring-boot:run
```
Frontend runs on: `http://localhost:8081`

### Test Streaming
1. Open browser to `http://localhost:8081`
2. Enter task: "Create a simple Spring Boot REST API"
3. Click "🚀 Start Streaming Task"
4. Watch real-time events appear as they occur

### Test with cURL
```bash
curl -X POST http://localhost:8080/api/agent/stream \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Create a Hello.java file","directoryPath":"/tmp/test"}' \
  --no-buffer
```

## API Examples

### Non-Streaming Request
```bash
curl -X POST http://localhost:8080/api/agent/process \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Create a REST API",
    "directoryPath": "/tmp/project",
    "useCollaboration": false
  }'
```

### Streaming Request
```bash
curl -X POST http://localhost:8080/api/agent/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "prompt": "Create a REST API",
    "directoryPath": "/tmp/project",
    "useCollaboration": false
  }' \
  --no-buffer
```

## Benefits

1. **Real-time Feedback**: Users see progress immediately
2. **Transparency**: Every AI decision and tool execution is visible
3. **Better UX**: No waiting for entire task to complete
4. **Debugging**: Easy to identify where issues occur
5. **Content Management**: File contents properly truncated for readability
6. **Professional Display**: Clean, color-coded event cards with animations

## Architecture Highlights

- **Reactive Streams**: Uses Project Reactor for non-blocking streaming
- **SSE Standard**: Server-Sent Events for reliable one-way communication
- **Thread Safety**: Proper sink management for concurrent event emission
- **Error Handling**: Comprehensive error catching and reporting
- **Backward Compatible**: Non-streaming endpoints still available
- **Smart Truncation**: Context-aware content truncation based on tool type
