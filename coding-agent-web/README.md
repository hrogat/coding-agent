# Coding Agent Web

Web UI for the AI-powered coding agents. This application provides a user-friendly interface to interact with the coding-agent backend service.

## Features

- **Modern Web Interface**: Clean, responsive UI built with HTML, CSS, and JavaScript
- **Real-time Communication**: Async requests to the backend using WebClient
- **Formatted Output**: Markdown rendering with syntax highlighting for code blocks
- **Directory Context Support**: Optional directory path input for context-aware responses
- **Agent Type Display**: Shows which agent (ANALYZE, CODE, BUGFIX) handled the request

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Running instance of `coding-agent` backend (port 8080)

## Setup

### 1. Start the Backend

First, ensure the `coding-agent` backend is running:

```bash
cd ../coding-agent
mvn spring-boot:run
```

The backend should be running on `http://localhost:8080`

### 2. Build the Web Application

```bash
mvn clean install
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The web application will start on `http://localhost:8081`

### 4. Access the UI

Open your browser and navigate to:

```
http://localhost:8081
```

## Usage

### Basic Request

1. Enter your coding task in the prompt field
2. Click "Submit Request"
3. View the formatted response from the AI agent

### With Directory Context

1. Enter your coding task in the prompt field
2. Provide a directory path (e.g., `/home/user/myproject/src`)
3. Click "Submit Request"
4. The agent will analyze your project files and provide context-aware responses

### Keyboard Shortcuts

- **Ctrl + Enter**: Submit the request

## Configuration

Configuration is managed in `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: coding-agent-web

server:
  port: 8081

coding-agent:
  backend-url: http://localhost:8080
```

### Changing the Backend URL

If your backend is running on a different host or port, update the `coding-agent.backend-url` property.

## Architecture

```
Browser
    ↓
WebController (serves HTML)
    ↓
JavaScript (app.js)
    ↓
ApiController (/api/submit)
    ↓
AgentClientService (WebClient)
    ↓
coding-agent backend (http://localhost:8080)
```

### Components

- **WebController**: Serves the main HTML page
- **ApiController**: REST endpoint for submitting requests
- **AgentClientService**: WebClient-based service to communicate with backend
- **index.html**: Main UI template with Thymeleaf
- **style.css**: Modern, responsive styling
- **app.js**: Client-side logic for form submission and result formatting

## UI Features

### Input Section

- **Prompt Field**: Multi-line textarea for describing coding tasks
- **Directory Path**: Optional field for providing project context
- **Submit Button**: Sends request with loading indicator

### Result Display

- **Agent Badge**: Color-coded badge showing which agent handled the request
  - ANALYZE: Blue
  - CODE: Green
  - BUGFIX: Red
- **Reasoning**: Shows why the request was classified for that agent
- **Formatted Content**: Markdown-rendered response with:
  - Syntax-highlighted code blocks
  - Proper heading hierarchy
  - Lists and emphasis
  - Clean typography

### Error Handling

- Connection errors to backend
- Invalid input validation
- User-friendly error messages

## Development

### Project Structure

```
coding-agent-web/
├── src/main/java/com/codingagent/web/
│   ├── CodingAgentWebApplication.java
│   ├── config/
│   │   └── AgentClientConfig.java
│   ├── controller/
│   │   ├── ApiController.java
│   │   └── WebController.java
│   ├── model/
│   │   ├── AgentRequest.java
│   │   └── AgentResponse.java
│   └── service/
│       └── AgentClientService.java
├── src/main/resources/
│   ├── static/
│   │   ├── css/
│   │   │   └── style.css
│   │   └── js/
│   │       └── app.js
│   ├── templates/
│   │   └── index.html
│   └── application.yml
└── pom.xml
```

### Running in Development

```bash
mvn spring-boot:run
```

### Building for Production

```bash
mvn clean package
java -jar target/coding-agent-web-1.0.0.jar
```

## Troubleshooting

### Backend Connection Issues

**Error**: "Failed to process request: Server error: 500"

**Solution**: 
- Ensure the `coding-agent` backend is running on port 8080
- Check the backend logs for errors
- Verify the `coding-agent.backend-url` in `application.yml`

### Port Already in Use

**Error**: "Port 8081 is already in use"

**Solution**:
- Change the port in `application.yml`:
  ```yaml
  server:
    port: 8082
  ```

### Styling Issues

**Solution**:
- Clear browser cache
- Check browser console for CSS loading errors
- Verify static resources are in `src/main/resources/static/`

## Browser Compatibility

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

## License

MIT License
