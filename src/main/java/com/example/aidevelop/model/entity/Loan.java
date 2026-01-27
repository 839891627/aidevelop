package com.example.aidevelop.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 借款表实体（精简演示版）
 * 用于 AI 聊天助手的学习和演示
 */
@Data
@Entity
@Table(name = "loan", indexes = {
    @Index(name = "idx_user_no", columnList = "user_no"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_create_time", columnList = "create_time")
})
public class Loan {

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
     * 产品编码
     */
    @Column(name = "product_code", length = 64)
    private String productCode;

    /**
     * 借款金额
     */
    @Column(name = "loan_amt", precision = 10, scale = 2)
    private BigDecimal loanAmt;

    /**
     * 年利率
     */
    @Column(name = "fee_rate", precision = 10, scale = 6)
    private BigDecimal feeRate;

    /**
     * 状态: INIT-初始, SUCCESS-成功, FAIL-失败, PENDING-借款中
     */
    @Column(name = "status", length = 20)
    private String status;

    /**
     * 放款成功时间
     */
    @Column(name = "loan_success_time")
    private LocalDateTime loanSuccessTime;

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
