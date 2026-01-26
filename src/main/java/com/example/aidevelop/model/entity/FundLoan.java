package com.example.aidevelop.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 借款表实体 - fund_loan
 */
@Data
@Entity
@Table(name = "fund_loan")
public class FundLoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 业务流水号
     */
    @Column(name = "biz_serial", unique = true, length = 64)
    private String bizSerial;

    /**
     * 三方请求号
     */
    @Column(name = "third_req_no", length = 64)
    private String thirdReqNo;

    /**
     * 三方响应号
     */
    @Column(name = "third_resp_no", length = 64)
    private String thirdRespNo;

    /**
     * 三方借据号
     */
    @Column(name = "third_loan_no", length = 64)
    private String thirdLoanNo;

    /**
     * 资方编号
     */
    @Column(name = "fund_source", length = 32)
    private String fundSource;

    /**
     * 资金包编码
     */
    @Column(name = "third_code", length = 32)
    private String thirdCode;

    /**
     * 授信单号
     */
    @Column(name = "credit_no", length = 50)
    private String creditNo;

    /**
     * 产品编码
     */
    @Column(name = "product_code", length = 64)
    private String productCode;

    /**
     * 产品版本
     */
    @Column(name = "product_ver", length = 64)
    private String productVer;

    /**
     * 账户编号
     */
    @Column(name = "cust_no", length = 64)
    private String custNo;

    /**
     * 用户号
     */
    @Column(name = "user_no", length = 64)
    private String userNo;

    /**
     * 借款金额
     */
    @Column(name = "loan_amt", precision = 10, scale = 2)
    private BigDecimal loanAmt;

    /**
     * 资方年利率
     */
    @Column(name = "fee_rate", precision = 10, scale = 6)
    private BigDecimal feeRate;

    /**
     * 借款结果: INIT-创建, SUCCESS-成功, FAIL-失败, PENDING-借款中
     */
    @Column(name = "status", length = 8)
    private String status;

    /**
     * 资账结清状态: LENDING-借款中, SETTLE-结清
     */
    @Column(name = "fund_status", length = 8)
    private String fundStatus;

    /**
     * 客账结清状态: LENDING-借款中, SETTLE-结清
     */
    @Column(name = "bill_status", length = 8)
    private String billStatus;

    /**
     * 客账结清时间
     */
    @Column(name = "bill_repay_time")
    private LocalDateTime billRepayTime;

    /**
     * 资账结清时间
     */
    @Column(name = "fund_repay_time")
    private LocalDateTime fundRepayTime;

    /**
     * 还款方式: 00-等额本金, 01-等额本息, 02-先息后本
     */
    @Column(name = "repay_type", length = 10)
    private String repayType;

    /**
     * 响应码
     */
    @Column(name = "resp_code", length = 20)
    private String respCode;

    /**
     * 响应消息
     */
    @Column(name = "resp_msg", length = 512)
    private String respMsg;

    /**
     * 三方响应码
     */
    @Column(name = "third_resp_code", length = 32)
    private String thirdRespCode;

    /**
     * 三方响应消息
     */
    @Column(name = "third_resp_msg", length = 512)
    private String thirdRespMsg;

    /**
     * 借款申请受理时间
     */
    @Column(name = "loan_accept_time")
    private LocalDateTime loanAcceptTime;

    /**
     * 放款成功时间
     */
    @Column(name = "loan_success_time")
    private LocalDateTime loanSuccessTime;

    /**
     * 合同号
     */
    @Column(name = "contract_no", length = 100)
    private String contractNo;

    /**
     * 借款期数
     */
    @Column(name = "term")
    private Integer term;

    /**
     * 借款期数
     */
    @Column(name = "loan_purpose", length = 64)
    private String loanPurpose;

    /**
     * 对客资方利率
     */
    @Column(name = "bill_fee_rate", precision = 7, scale = 6)
    private BigDecimal billFeeRate;

    /**
     * 子产品编码
     */
    @Column(name = "sub_product_code", length = 128)
    private String subProductCode;

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

    /**
     * 合同编号（合同系统）
     */
    @Column(name = "contract_number", length = 64)
    private String contractNumber;
}
