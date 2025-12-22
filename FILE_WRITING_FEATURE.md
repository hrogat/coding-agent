# File Writing Feature

## Overview

The coding agent now has the ability to automatically create and modify files on the filesystem. When you ask the agent to create a project or generate code, it will not only provide the code in the response but also write the files directly to disk.

## How It Works

### 1. Agent Response Format

When the Code Agent generates files, it uses a specific format that the system can parse:

```
FILE: path/to/file.ext
```language
file content here
```
```

Example:
```
FILE: src/main/java/com/example/MyClass.java
```java
package com.example;

public class MyClass {
    public static void main(String[] args) {
        System.out.println("Hello World");
    }
}
```
```

### 2. Automatic File Creation

The system automatically:
1. Parses the AI response for file operations
2. Extracts file paths and content
3. Creates parent directories if needed
4. Writes files to the filesystem
5. Reports which files were successfully created

### 3. Directory Path Resolution

- **Absolute paths**: Used as-is (e.g., `/home/user/project/src/Main.java`)
- **Relative paths**: Combined with the `directoryPath` from the request
- If no `directoryPath` is provided, relative paths are used from the current working directory

## Components

### FileWriterService

Handles the actual file system operations:
- Creates directories as needed
- Writes file content
- Handles errors gracefully
- Supports CREATE, UPDATE, and DELETE operations

### FileOperationParser

Parses AI responses to extract file operations:
- Detects file blocks in the response
- Extracts file paths and content
- Resolves relative paths
- Supports multiple file formats

### OrchestratorService Integration

The orchestrator automatically:
1. Receives the AI response
2. Checks for file operations
3. Parses and executes file operations
4. Returns the list of files written

## API Response

The API response now includes:

```json
{
  "agentType": "CODE",
  "result": "AI response with code and explanations",
  "reasoning": "Request classified as CODE task",
  "filesWritten": [
    "/home/user/project/src/Main.java",
    "/home/user/project/pom.xml"
  ],
  "fileCount": 2
}
```

## Usage Examples

### Example 1: Create a Simple Java Project

**Request:**
```json
{
  "prompt": "Create a simple Java Hello World project with Maven",
  "directoryPath": "/home/user/my-project"
}
```

**Result:**
- Files are automatically created in `/home/user/my-project/`
- Response includes the list of created files
- Web UI displays a "Files Written" section

### Example 2: Add a New Class to Existing Project

**Request:**
```json
{
  "prompt": "Create a UserService class with CRUD operations",
  "directoryPath": "/home/user/my-project/src/main/java/com/example"
}
```

**Result:**
- `UserService.java` is created in the specified directory
- Existing files are not modified unless explicitly requested

## Security Considerations

### File System Access

The agent has write access to the filesystem, so:
- **Use with caution** in production environments
- Consider running in a containerized environment
- Implement additional access controls if needed

### Path Validation

Currently, the system:
- Creates parent directories automatically
- Overwrites existing files if they exist
- Does not validate paths against a whitelist

**Recommended Improvements:**
- Add path whitelisting
- Require confirmation for overwrites
- Implement file size limits
- Add user permissions checking

## Configuration

No additional configuration is required. The feature is enabled by default.

To disable file writing (if needed in the future), you could add:

```yaml
coding-agent:
  file-writing:
    enabled: false
```

## Limitations

1. **No Undo**: Once files are written, there's no automatic undo mechanism
2. **Overwrites**: Existing files are overwritten without warning
3. **No Backup**: No automatic backup of existing files
4. **Parse Dependency**: Relies on AI formatting responses correctly

## Best Practices

### For Users

1. **Use version control**: Always have your project in Git before using the agent
2. **Review before applying**: Check the AI response before files are written
3. **Provide clear paths**: Use absolute paths when possible
4. **Test in safe directories**: Try the feature in a test directory first

### For Developers

1. **Add confirmation prompts**: Consider adding user confirmation for file operations
2. **Implement backups**: Create backups before overwriting files
3. **Add logging**: Log all file operations for audit purposes
4. **Validate paths**: Implement path validation and whitelisting

## Future Enhancements

Potential improvements:
- [ ] Confirmation prompts for file operations
- [ ] Automatic backups before overwrites
- [ ] Path whitelisting configuration
- [ ] Dry-run mode to preview changes
- [ ] Integration with Git for automatic commits
- [ ] File diff display in the UI
- [ ] Rollback functionality
