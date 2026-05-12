-- ============================================
-- Chat Memory 持久化表
-- ============================================

USE ai_develop;

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息ID（去重）',
    conversation_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    role VARCHAR(16) NOT NULL COMMENT '消息角色: SYSTEM/USER/ASSISTANT',
    content TEXT NOT NULL COMMENT '消息内容',
    model VARCHAR(100) NULL COMMENT '模型名称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息时间',
    UNIQUE KEY uk_message_id (message_id),
    INDEX idx_conv_created (conversation_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话消息表';
