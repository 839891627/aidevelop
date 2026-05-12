-- ============================================
-- Prompt Registry 表结构
-- ============================================

USE ai_develop;

CREATE TABLE IF NOT EXISTS prompt_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    prompt_key VARCHAR(100) NOT NULL COMMENT 'Prompt 业务标识，如 system.default',
    version INT NOT NULL COMMENT '版本号，按 prompt_key + env 递增',
    status VARCHAR(20) NOT NULL COMMENT '状态：DRAFT/ACTIVE/ARCHIVED',
    content TEXT NOT NULL COMMENT 'Prompt 内容',
    variables_json TEXT NULL COMMENT '变量定义(JSON)',
    model_scope VARCHAR(100) NULL COMMENT '适用模型范围',
    env VARCHAR(20) NOT NULL COMMENT '环境：dev/staging/prod',
    created_by VARCHAR(64) NULL COMMENT '创建者',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_prompt_key_env_version (prompt_key, env, version),
    INDEX idx_prompt_key_env_status (prompt_key, env, status),
    INDEX idx_prompt_key_env_version (prompt_key, env, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Prompt 模板注册表';

CREATE TABLE IF NOT EXISTS prompt_publish_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    prompt_key VARCHAR(100) NOT NULL COMMENT 'Prompt 业务标识',
    env VARCHAR(20) NOT NULL COMMENT '环境',
    action VARCHAR(20) NOT NULL COMMENT '动作：PUBLISH/ROLLBACK',
    from_version INT NULL COMMENT '来源版本',
    to_version INT NOT NULL COMMENT '目标版本',
    remark VARCHAR(500) NULL COMMENT '备注',
    operator VARCHAR(64) NULL COMMENT '操作人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_prompt_log_key_env_time (prompt_key, env, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Prompt 发布日志';

-- ============================================
-- 初始化 Prompt（dev 环境）
-- 说明：
-- 1) 仅初始化 version=1
-- 2) 可重复执行：若已存在则更新内容并保持 ACTIVE
-- ============================================

INSERT INTO prompt_template
    (prompt_key, version, status, content, variables_json, model_scope, env, created_by)
VALUES
    (
        'system.default',
        1,
        'ACTIVE',
        '你是一个专业的金融助贷系统 AI 助手。\n\n你的职责：\n1. 准确回答贷款业务问题。\n2. 优先使用已提供的业务规则和检索信息。\n3. 需要结构化数据时触发可用函数工具。\n\n回答要求：\n- 结论清晰，步骤简洁。\n- 不编造不存在的信息。\n- 信息不足时明确说明并给出下一步建议。',
        NULL,
        'openai-compatible',
        'dev',
        'system'
    ),
    (
        'rag.qa',
        1,
        'ACTIVE',
        '你将收到用户问题和检索到的参考资料。\n请严格基于参考资料回答：\n1. 优先引用与问题最相关的条目。\n2. 若参考资料无法支持结论，明确说明信息不足。\n3. 不要编造规则、阈值、流程或数值。\n4. 输出先给结论，再给依据。',
        NULL,
        'openai-compatible',
        'dev',
        'system'
    ),
    (
        'function.calling',
        1,
        'ACTIVE',
        '你可以调用后端函数查询真实业务数据。\n调用原则：\n1. 用户询问具体记录、金额、状态时优先调用函数。\n2. 参数不完整时先向用户补齐关键字段。\n3. 函数返回后先核对结果再作答。\n4. 当函数结果为空时明确说明未查询到数据。',
        NULL,
        'openai-compatible',
        'dev',
        'system'
    )
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    content = VALUES(content),
    variables_json = VALUES(variables_json),
    model_scope = VALUES(model_scope),
    created_by = VALUES(created_by),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO prompt_publish_log (prompt_key, env, action, from_version, to_version, remark, operator)
SELECT 'system.default', 'dev', 'PUBLISH', NULL, 1, '初始化版本', 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM prompt_publish_log
    WHERE prompt_key = 'system.default' AND env = 'dev' AND action = 'PUBLISH' AND to_version = 1
);

INSERT INTO prompt_publish_log (prompt_key, env, action, from_version, to_version, remark, operator)
SELECT 'rag.qa', 'dev', 'PUBLISH', NULL, 1, '初始化版本', 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM prompt_publish_log
    WHERE prompt_key = 'rag.qa' AND env = 'dev' AND action = 'PUBLISH' AND to_version = 1
);

INSERT INTO prompt_publish_log (prompt_key, env, action, from_version, to_version, remark, operator)
SELECT 'function.calling', 'dev', 'PUBLISH', NULL, 1, '初始化版本', 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM prompt_publish_log
    WHERE prompt_key = 'function.calling' AND env = 'dev' AND action = 'PUBLISH' AND to_version = 1
);
