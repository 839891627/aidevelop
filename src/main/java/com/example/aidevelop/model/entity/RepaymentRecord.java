package com.example.aidevelop.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 还款记录表实体（精简演示版）
 * 用于 AI 聊天助手的学习和演示
 */
@Data
@Entity
@Table(name = "repayment_record", indexes = {
    @Index(name = "idx_user_no", columnList = "user_no"),
    @Index(name = "idx_loan_no", columnList = "loan_no"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_create_time", columnList = "create_time")
})
public class RepaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 业务流水号
     */
    @Column(name = "biz_serial", unique = true, length = 64)
    private String bizSerial;

    /**
     * 用户编号
     */
    @Column(name = "user_no", length = 64)
    private String userNo;

    /**
     * 借款编号
     */
    @Column(name = "loan_no", length = 64)
    private String loanNo;

    /**
     * 还款总金额
     */
    @Column(name = "total_amt", precision = 17, scale = 2)
    private BigDecimal totalAmt;

    /**
     * 还款类型: AD-提前还款, DUE-到期还款, OVER-逾期还款, SETTLE-提前结清
     */
    @Column(name = "repay_type", length = 20)
    private String repayType;

    /**
     * 状态: INIT-初始, PENDING-还款中, SUCCESS-成功, FAIL-失败
     */
    @Column(name = "status", length = 20)
    private String status;

    /**
     * 还款成功时间
     */
    @Column(name = "repay_success_time")
    private LocalDateTime repaySuccessTime;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
