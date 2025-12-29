package com.codingagent.service.tool;

/**
 * Thread-local context for tool execution that provides access to the base directory path.
 */
public class ToolExecutionContext {
    
    private static final ThreadLocal<String> baseDirectory = new ThreadLocal<>();
    
    public static void setBaseDirectory(String directory) {
        baseDirectory.set(directory);
    }
    
    public static String getBaseDirectory() {
        return baseDirectory.get();
    }
    
    public static void clear() {
        baseDirectory.remove();
    }
}
