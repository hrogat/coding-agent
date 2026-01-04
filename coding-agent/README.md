# Coding Agent

AI-powered coding agents using Mistral AI and Spring Boot. This application provides specialized agents for different coding tasks:

- **Analyze Agent**: Code analysis, review, and quality assessment
- **Code Agent**: Code generation and implementation
- **Bugfix Agent**: Debugging and error resolution

An orchestrator automatically routes requests to the appropriate agent based on the task.

## Key Features

- **Intelligent Agent Routing**: Automatically classifies tasks and routes to the appropriate agent
- **Directory Context**: Agents can access and analyze files in a specified directory
- **File System Integration**: Automatically reads project structure and file contents for context-aware responses
- **File Writing Capability**: Agents can automatically create and modify files on the filesystem

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Mistral AI API key

## Setup

### 1. Get Mistral AI API Key

Sign up at [Mistral AI](https://mistral.ai/) and obtain your API key.

### 2. Configure Environment

Set your Mistral AI API key as an environment variable:

```bash
export MISTRAL_API_KEY=your_api_key_here
```

Or create a `.env` file in the project root:

```env
MISTRAL_API_KEY=your_api_key_here
```

### 3. Build the Project

```bash
mvn clean install
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Usage

### API Endpoint

**POST** `/api/agent/process`

**Request Body:**

```json
{
  "prompt": "Your coding task description here",
  "directoryPath": "/path/to/your/project" // Optional: provides file context to the agent
}
```

**Response:**

```json
{
  "agentType": "CODE",
  "result": "Agent's response...",
  "reasoning": "Request classified as CODE task"
}
```

### Example Requests

#### Code Generation (Simple)

```bash
curl -X POST http://localhost:8080/api/agent/process \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Create a Java function to calculate factorial"}'
```

#### Code Generation with Directory Context

```bash
curl -X POST http://localhost:8080/api/agent/process \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Add a new REST endpoint for user management", "directoryPath": "/home/user/myproject/src"}'
```

#### Code Analysis with Directory Context

```bash
curl -X POST http://localhost:8080/api/agent/process \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Analyze the code quality and suggest improvements", "directoryPath": "/home/user/myproject/src"}'
```

#### Bug Fixing with Directory Context

```bash
curl -X POST http://localhost:8080/api/agent/process \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Fix the authentication bug in the login controller", "directoryPath": "/home/user/myproject/src"}'
```

## Architecture

```text
User Request
    ↓
AgentController
    ↓
OrchestratorService (classifies request)
    ↓
├─→ AnalyzeAgent
├─→ CodeAgent
└─→ BugfixAgent
```

### Components

- **AgentController**: REST API endpoint for receiving user requests
- **OrchestratorService**: Routes requests to appropriate agents using AI classification
- **FileSystemService**: Reads and processes directory contents to provide context
- **Agent Interface**: Common interface for all specialized agents
- **AnalyzeAgent**: Handles code analysis tasks
- **CodeAgent**: Handles code generation tasks
- **BugfixAgent**: Handles debugging tasks

### Directory Context Feature

The directory context feature allows agents to access and analyze files in a specified directory, making them context-aware:

**How it works:**

1. User provides a `directoryPath` in the request
2. `FileSystemService` scans the directory and builds a context containing:
   - Directory structure (tree view up to 3 levels deep)
   - Contents of relevant source files (Java, XML, YAML, etc.)
3. Context is passed to the selected agent along with the user prompt
4. Agent uses both the prompt and file context to generate informed responses

**Limitations:**

- Maximum 50 files processed per request
- Maximum 1MB per file
- Automatically skips binary files, build directories, and common ignore patterns
- Supports common text file formats: `.java`, `.xml`, `.yml`, `.properties`, `.md`, `.json`, etc.

**Use Cases:**

- **Code Analysis**: Analyze entire project structure and identify patterns
- **Code Generation**: Generate code that follows existing project conventions
- **Bug Fixing**: Understand the full context of a bug across multiple files
- **Refactoring**: Suggest improvements based on the actual codebase

## Configuration

Configuration is managed in `src/main/resources/application.yml`:

```yaml
spring:
  ai:
    mistralai:
      api-key: ${MISTRAL_API_KEY}
      chat:
        options:
          model: mistral-large-latest
          temperature: 0.7
```

### Available Models

You can change the model in `application.yml`:

- `mistral-large-latest` (default)
- `mistral-medium-latest`
- `mistral-small-latest`

## Development

### Project Structure

```text
coding-agent/
├── src/main/java/com/codingagent/
│   ├── CodingAgentApplication.java
│   ├── controller/
│   │   └── AgentController.java
│   ├── service/
│   │   ├── OrchestratorService.java
│   │   └── agent/
│   │       ├── Agent.java
│   │       ├── AnalyzeAgent.java
│   │       ├── CodeAgent.java
│   │       └── BugfixAgent.java
│   └── model/
│       ├── AgentRequest.java
│       ├── AgentResponse.java
│       └── AgentType.java
├── src/main/resources/
│   └── application.yml
└── pom.xml
```

### Adding New Agents

1. Create a new agent class implementing the `Agent` interface
2. Add a new `AgentType` enum value
3. Implement the `execute()` method with your custom system prompt
4. Spring will automatically register it with the orchestrator

### Testing

Run tests with:

```bash
mvn test
```

## Troubleshooting

### API Key Issues

- Ensure `MISTRAL_API_KEY` environment variable is set
- Verify the API key is valid at [Mistral AI Console](https://console.mistral.ai/)

### Connection Issues

- Check your internet connection
- Verify Mistral AI API is accessible
- Review logs in the console for detailed error messages

### Classification Issues

- The orchestrator uses AI to classify requests
- If misclassification occurs, try rephrasing your prompt more clearly
- Check logs to see which agent was selected

## License

MIT License
