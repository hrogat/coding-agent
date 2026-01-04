# Tool-Based Agent Architecture

## Overview
The coding-agent system has been refactored to use a **tool-based architecture** where agents guide the LLM to use specific tools rather than directly generating code. This approach provides better control, observability, and modularity.

## Architecture

### Tools Available
All agents have access to the following tools:

1. **list_files** - Lists files and directories in a path
2. **read_file** - Reads file content
3. **write_file** - Writes content to files (creates directories as needed)
4. **log_thought** - Logs reasoning and thought process
5. **finish_task** - Signals task completion (REQUIRED at end)

### Tool Interface
```java
public interface Tool {
    String getName();
    String getDescription();
    String execute(String parameters);
}
```

### Tool Usage Format
Agents instruct the LLM to call tools using this format:
```
TOOL: tool_name {"param": "value"}
```

Example:
```
TOOL: log_thought {"thought": "I need to create a User class"}
TOOL: write_file {"path": "src/User.java", "content": "public class User {...}"}
TOOL: finish_task {"summary": "Created User.java"}
```

## Agent Workflow

### 1. ToolBasedAgent (Base Class)
- Manages the tool execution loop (max 20 iterations)
- Parses tool calls from LLM responses
- Executes tools and provides results back to LLM
- Maintains conversation history
- Enforces `finish_task` requirement

### 2. Specialized Agents
All agents extend `ToolBasedAgent`:

**CodeAgent**
- Generates clean, well-documented code
- Uses tools to explore, read, and write files
- Follows best practices and design patterns

**AnalyzeAgent**
- Analyzes code structure and quality
- Uses tools to examine codebase
- Provides actionable feedback

**BugfixAgent**
- Identifies and fixes bugs
- Documents debugging process via `log_thought`
- Applies fixes using `write_file`

## Key Benefits

### 1. Observability
- Every action is logged (file reads, writes, thoughts)
- Clear audit trail of agent decisions
- Easy to debug agent behavior

### 2. Control
- Tools enforce constraints (file size limits, path validation)
- Agent cannot perform arbitrary operations
- Explicit task completion via `finish_task`

### 3. Modularity
- Tools are independent, testable components
- Easy to add new tools without changing agents
- Tools can be reused across different agents

### 4. Safety
- File operations are validated and logged
- Errors are caught and reported
- Maximum iteration limit prevents infinite loops

## Implementation Details

### Tool Implementations

**ListFilesTool**
- Lists directory contents
- Marks items as [DIR] or [FILE]
- Validates path exists and is a directory

**ReadFileTool**
- Reads file content as string
- Enforces 1MB size limit
- Validates file exists and is readable

**WriteFileTool**
- Creates parent directories automatically
- Overwrites existing files
- Logs file size and path

**LogThoughtTool**
- Logs agent reasoning to console
- Helps understand agent decision-making
- Useful for debugging and transparency

**FinishTaskTool**
- Signals task completion
- Provides summary of work done
- Required to end agent execution

### Execution Flow

1. **Agent receives request** with user prompt and directory context
2. **System prompt** includes tool descriptions and instructions
3. **LLM generates response** with tool calls
4. **ToolBasedAgent parses** tool calls using regex
5. **Tools are executed** and results added to conversation
6. **Loop continues** until `finish_task` is called or max iterations reached
7. **Full conversation history** returned as result

### Example Execution

```
User Request: Create a simple Calculator class

Iteration 1:
Assistant: TOOL: log_thought {"thought": "I'll create a Calculator.java file"}
Tool Result: Thought logged

Iteration 2:
Assistant: TOOL: write_file {"path": "Calculator.java", "content": "public class Calculator {...}"}
Tool Result: Success: File written to Calculator.java

Iteration 3:
Assistant: TOOL: finish_task {"summary": "Created Calculator.java with basic operations"}
Tool Result: TASK_COMPLETE: Created Calculator.java with basic operations
```

## Configuration

Tools are automatically discovered and injected via Spring:
- All `@Component` classes implementing `Tool` are collected
- Injected into agents via constructor
- No manual configuration required

## File Structure

```
service/
├── tool/
│   ├── Tool.java                 # Tool interface
│   ├── ListFilesTool.java        # List files implementation
│   ├── ReadFileTool.java         # Read file implementation
│   ├── WriteFileTool.java        # Write file implementation
│   ├── LogThoughtTool.java       # Log thought implementation
│   └── FinishTaskTool.java       # Finish task implementation
└── agent/
    ├── ToolBasedAgent.java       # Base agent with tool loop
    ├── CodeAgent.java            # Code generation agent
    ├── AnalyzeAgent.java         # Code analysis agent
    └── BugfixAgent.java          # Bug fixing agent
```

## Migration Notes

### Removed Components
- `BaseAgent.java` - Replaced by `ToolBasedAgent`
- `FileOperationParser.java` - No longer needed (tools handle file ops)
- `FileWriterService.java` - Replaced by `WriteFileTool`

### Updated Components
- All agents now extend `ToolBasedAgent` instead of `BaseAgent`
- `OrchestratorService` simplified - no file operation parsing
- Agents receive `List<Tool>` in constructor

## Compilation Status
✅ **Build successful** - 28 source files compiled without errors

## Future Enhancements

Potential new tools to add:
- `delete_file` - Delete files
- `move_file` - Move/rename files
- `run_command` - Execute shell commands
- `search_code` - Search for patterns in code
- `format_code` - Auto-format code files
