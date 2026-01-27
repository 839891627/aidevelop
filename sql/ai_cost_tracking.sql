-- ============================================
-- AI 成本追踪数据库表
-- ============================================

USE ai_develop;

-- AI 调用日志表
CREATE TABLE IF NOT EXISTS ai_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    -- 会话信息
    session_id VARCHAR(64) COMMENT '会话ID（用于关联一次对话）',
    user_id VARCHAR(64) COMMENT '用户ID',

    -- 模型信息
    model_name VARCHAR(50) NOT NULL COMMENT '模型名称: deepseek-chat, embedding-2等',
    model_type VARCHAR(20) NOT NULL COMMENT '模型类型: CHAT, EMBEDDING',
    provider VARCHAR(20) NOT NULL COMMENT '提供商: OPENAI, ZHIPUAI',

    -- Token 统计
    prompt_tokens INT DEFAULT 0 COMMENT '输入Token数',
    completion_tokens INT DEFAULT 0 COMMENT '输出Token数',
    total_tokens INT DEFAULT 0 COMMENT '总Token数',

    -- 成本信息
    cost DECIMAL(10, 6) DEFAULT 0 COMMENT '本次调用成本（元）',

    -- 性能指标
    latency_ms BIGINT COMMENT '响应耗时（毫秒）',

    -- 状态
    status VARCHAR(20) NOT NULL COMMENT '状态: SUCCESS, FAILURE, TIMEOUT',
    error_message TEXT COMMENT '错误信息',

    -- 时间戳
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_session (session_id),
    INDEX idx_user (user_id),
    INDEX idx_model (model_name),
    INDEX idx_created (created_time),
    INDEX idx_provider (provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI调用日志表';

-- 每日成本统计表（可选，用于加速查询）
CREATE TABLE IF NOT EXISTS ai_daily_cost_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stats_date DATE NOT NULL UNIQUE COMMENT '统计日期',
    provider VARCHAR(20) NOT NULL COMMENT '提供商',
    model_name VARCHAR(50) NOT NULL COMMENT '模型名称',
    call_count INT DEFAULT 0 COMMENT '调用次数',
    total_tokens BIGINT DEFAULT 0 COMMENT '总Token数',
    total_cost DECIMAL(10, 6) DEFAULT 0 COMMENT '总成本',
    avg_latency_ms BIGINT COMMENT '平均响应时间',
    success_rate DECIMAL(5, 4) COMMENT '成功率',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_date_provider_model (stats_date, provider, model_name),
    INDEX idx_date (stats_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI每日成本统计表';
