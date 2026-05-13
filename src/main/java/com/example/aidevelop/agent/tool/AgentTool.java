package com.example.aidevelop.agent.tool;

import java.util.Map;

/**
 * Agent 工具统一接口。
 */
public interface AgentTool {

    String name();

    Object execute(Map<String, Object> args);
}
