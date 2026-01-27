-- ============================================
-- 精简的演示数据库表结构
-- 用于 AI 聊天助手的学习和演示
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ai_develop DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_develop;

-- ============================================
-- 借款表（精简版）
-- ============================================
DROP TABLE IF EXISTS loan;
CREATE TABLE loan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    -- 基本信息
    biz_serial VARCHAR(64) UNIQUE NOT NULL COMMENT '业务流水号',
    user_no VARCHAR(64) NOT NULL COMMENT '用户编号',
    product_code VARCHAR(64) COMMENT '产品编码',

    -- 金额信息
    loan_amt DECIMAL(10, 2) NOT NULL COMMENT '借款金额',
    fee_rate DECIMAL(10, 6) COMMENT '年利率',

    -- 状态信息
    status VARCHAR(20) NOT NULL COMMENT '状态: INIT-初始, SUCCESS-成功, FAIL-失败, PENDING-借款中',
    loan_success_time DATETIME COMMENT '放款成功时间',

    -- 时间戳
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_user_no (user_no),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借款表（演示）';

-- ============================================
-- 还款记录表（精简版）
-- ============================================
DROP TABLE IF EXISTS repayment_record;
CREATE TABLE repayment_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    -- 基本信息
    biz_serial VARCHAR(64) UNIQUE NOT NULL COMMENT '业务流水号',
    user_no VARCHAR(64) NOT NULL COMMENT '用户编号',
    loan_no VARCHAR(64) NOT NULL COMMENT '借款编号',

    -- 还款信息
    total_amt DECIMAL(17, 2) NOT NULL COMMENT '还款总金额',
    repay_type VARCHAR(20) COMMENT '还款类型: AD-提前, DUE-到期, OVER-逾期, SETTLE-结清',

    -- 状态信息
    status VARCHAR(20) NOT NULL COMMENT '状态: INIT-初始, PENDING-还款中, SUCCESS-成功, FAIL-失败',
    repay_success_time DATETIME COMMENT '还款成功时间',

    -- 时间戳
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_user_no (user_no),
    INDEX idx_loan_no (loan_no),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='还款记录表（演示）';

-- ============================================
-- 插入演示数据
-- ============================================

-- 插入借款数据
INSERT INTO loan (biz_serial, user_no, product_code, loan_amt, fee_rate, status, loan_success_time) VALUES
('LOAN20250127001', 'USER001', 'PROD_VIP_001', 5000.00, 0.150000, 'SUCCESS', '2025-01-20 10:30:00'),
('LOAN20250127002', 'USER002', 'PROD_VIP_001', 10000.00, 0.180000, 'SUCCESS', '2025-01-21 14:20:00'),
('LOAN20250127003', 'USER003', 'PROD_STD_001', 3000.00, 0.120000, 'PENDING', NULL),
('LOAN20250127004', 'USER001', 'PROD_STD_001', 2000.00, 0.120000, 'SUCCESS', '2025-01-22 09:15:00'),
('LOAN20250127005', 'USER004', 'PROD_VIP_001', 15000.00, 0.180000, 'FAIL', NULL);

-- 插入还款数据
INSERT INTO repayment_record (biz_serial, user_no, loan_no, total_amt, repay_type, status, repay_success_time) VALUES
('REPAY20250127001', 'USER001', 'LOAN20250127001', 5200.00, 'DUE', 'SUCCESS', '2025-01-25 16:00:00'),
('REPAY20250127002', 'USER002', 'LOAN20250127002', 10500.00, 'DUE', 'PENDING', NULL),
('REPAY20250127003', 'USER001', 'LOAN20250127004', 2100.00, 'AD', 'SUCCESS', '2025-01-24 11:30:00');

-- ============================================
-- 查询示例（用于测试）
-- ============================================

-- 查询用户的借款记录
-- SELECT * FROM loan WHERE user_no = 'USER001';

-- 查询用户的还款记录
-- SELECT * FROM repayment_record WHERE user_no = 'USER001';

-- 查询逾期未还款的借款
-- SELECT * FROM loan WHERE status = 'SUCCESS' AND loan_success_time < DATE_SUB(NOW(), INTERVAL 30 DAY);
