-- ============================================
-- Agent 执行 Trace 数据库表
-- ============================================

USE ai_develop;

CREATE TABLE IF NOT EXISTS agent_trace (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    trace_id VARCHAR(128) NOT NULL UNIQUE COMMENT 'Agent traceId',
    conversation_id VARCHAR(64) COMMENT '会话ID',
    route_type VARCHAR(32) COMMENT '路由类型',
    mode VARCHAR(32) COMMENT '执行模式: SINGLE, MULTI',
    status VARCHAR(32) NOT NULL COMMENT '执行状态',
    failure_reason VARCHAR(64) COMMENT '失败原因分类',
    user_message TEXT COMMENT '用户原始问题',
    final_answer TEXT COMMENT '最终回答',
    completed TINYINT(1) DEFAULT 1 COMMENT '是否完成',
    executed_steps INT DEFAULT 0 COMMENT '执行步骤数',
    response_time_ms BIGINT COMMENT '总耗时',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_agent_trace_id (trace_id),
    INDEX idx_agent_trace_conversation (conversation_id, created_time),
    INDEX idx_agent_trace_created (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent执行Trace表';

CREATE TABLE IF NOT EXISTS agent_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    trace_id VARCHAR(128) NOT NULL COMMENT 'Agent traceId',
    step_index INT NOT NULL COMMENT '步骤序号',
    round_index INT DEFAULT 0 COMMENT '规划轮次',
    action_type VARCHAR(32) NOT NULL COMMENT '步骤类型',
    step_status VARCHAR(32) COMMENT '步骤状态',
    failure_reason VARCHAR(64) COMMENT '失败原因分类',
    tool_name VARCHAR(128) COMMENT '工具名称',
    tool_input_json TEXT COMMENT '工具输入JSON',
    tool_output TEXT COMMENT '步骤输出',
    latency_ms BIGINT COMMENT '步骤耗时',
    success TINYINT(1) DEFAULT 1 COMMENT '是否成功',
    error_message TEXT COMMENT '错误信息',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_agent_step_trace (trace_id, step_index),
    INDEX idx_agent_step_action (action_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent执行步骤表';
