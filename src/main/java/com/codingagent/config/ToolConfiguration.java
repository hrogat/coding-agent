package com.codingagent.config;

import com.codingagent.service.tool.FinishTaskTool;
import com.codingagent.service.tool.ListFilesTool;
import com.codingagent.service.tool.LogThoughtTool;
import com.codingagent.service.tool.ReadFileTool;
import com.codingagent.service.tool.Tool;
import com.codingagent.service.tool.WriteFileTool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ToolConfiguration {

    @Bean
    @Qualifier("analysisTools")
    public List<Tool> analysisTools(LogThoughtTool logThoughtTool,
                                    ListFilesTool listFilesTool,
                                    ReadFileTool readFileTool,
                                    FinishTaskTool finishTaskTool) {
        return List.of(logThoughtTool, listFilesTool, readFileTool, finishTaskTool);
    }

    @Bean
    @Qualifier("codeTools")
    public List<Tool> codeTools(LogThoughtTool logThoughtTool,
                                ListFilesTool listFilesTool,
                                ReadFileTool readFileTool,
                                WriteFileTool writeFileTool,
                                FinishTaskTool finishTaskTool) {
        return List.of(logThoughtTool, listFilesTool, readFileTool, writeFileTool, finishTaskTool);
    }

    @Bean
    @Qualifier("bugfixTools")
    public List<Tool> bugfixTools(LogThoughtTool logThoughtTool,
                                  ListFilesTool listFilesTool,
                                  ReadFileTool readFileTool,
                                  WriteFileTool writeFileTool,
                                  FinishTaskTool finishTaskTool) {
        return List.of(logThoughtTool, listFilesTool, readFileTool, writeFileTool, finishTaskTool);
    }
}
