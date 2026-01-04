package com.codingagent.service.tool;

public interface Tool {
    String getName();
    String getDescription();
    String execute(String parameters);
}
