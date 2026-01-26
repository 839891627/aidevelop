package com.example.aidevelop.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 还款记录表实体 - fund_repay_record
 */
@Data
@Entity
@Table(name = "fund_repay_record", indexes = {
    @Index(name = "idx_fund_repay_record_loan_no", columnList = "loan_no, fund_source, third_code"),
    @Index(name = "idx_fund_req_no", columnList = "fund_req_no"),
    @Index(name = "idx_third_resp_no", columnList = "third_resp_no"),
    @Index(name = "idx_update_time", columnList = "update_time"),
    @Index(name = "nm_create_time", columnList = "create_time")
})
public class FundRepayRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务流水号 */
    @Column(name = "biz_serial", unique = true, length = 64)
    private String bizSerial;

    /** 资方还款申请流水号 */
    @Column(name = "fund_req_no", length = 64)
    private String fundReqNo;

    /** 外部借款流水号 */
    @Column(name = "third_resp_no", length = 64)
    private String thirdRespNo;

    /** 三方借据号 */
    @Column(name = "third_loan_no", length = 64)
    private String thirdLoanNo;

    /** 资方编号 */
    @Column(name = "fund_source", length = 30)
    private String fundSource;

    /** 资金包编码 */
    @Column(name = "third_code", length = 30)
    private String thirdCode;

    /** 用户编号 */
    @Column(name = "user_no", length = 64)
    private String userNo;

    /** 账户编号 */
    @Column(name = "cust_no", length = 64)
    private String custNo;

    /** 卡系统ID */
    @Column(name = "card_id", length = 20)
    private String cardId;

    /** 银行卡号 */
    @Column(name = "bankcard_no", length = 64)
    private String bankcardNo;

    /** 还款总金额 */
    @Column(name = "total_amt", precision = 17, scale = 6)
    private BigDecimal totalAmt;

    /** 还款起始期数 */
    @Column(name = "begin_term", length = 64)
    private String beginTerm;

    /** 还款截止期数 */
    @Column(name = "end_term", length = 64)
    private String endTerm;

    /** 还款状态：INIT-初始, PENDING-还款中, SUCCESS-成功, FAIL-失败 */
    @Column(name = "status", length = 10)
    private String status;

    /** 信飞映射原因 */
    @Column(name = "resp_msg", length = 512)
    private String respMsg;

    /** 业务响应码 */
    @Column(name = "resp_code", length = 20)
    private String respCode;

    /** 资方响应原始信息 */
    @Column(name = "third_resp_code", length = 20)
    private String thirdRespCode;

    /** 资方原始原因 */
    @Column(name = "third_resp_msg", length = 512)
    private String thirdRespMsg;

    /** 还款类型：AD-提前还款, DUE-到期还款, OVER-逾期还款, SETTLE-提前结清 */
    @Column(name = "repay_type", length = 20)
    private String repayType;

    /** 还款方式：ONLINE-线上, OFFLINE-线下 */
    @Column(name = "repay_method", length = 10)
    private String repayMethod;

    /** 还款模式：BT-资方批扣, WBT-我方批扣, NM-正常还款 */
    @Column(name = "repay_mode", length = 10)
    private String repayMode;

    /** 还款申请受理时间 */
    @Column(name = "repay_accept_time")
    private LocalDateTime repayAcceptTime;

    /** 还款成功时间 */
    @Column(name = "repay_success_time")
    private LocalDateTime repaySuccessTime;

    /** 实际还款总金额 */
    @Column(name = "act_total_amt", precision = 17, scale = 6)
    private BigDecimal actTotalAmt;

    /** 支付渠道 */
    @Column(name = "pay_channel", length = 20)
    private String payChannel;

    /** 支付协议号 */
    @Column(name = "third_pay_serial", length = 64)
    private String thirdPaySerial;

    /** 交易类型 */
    @Column(name = "trans_type", length = 8)
    private String transType;

    /** 交易子类型 */
    @Column(name = "sub_trans_type", length = 16)
    private String subTransType;

    /** 借款编号 */
    @Column(name = "loan_no", length = 64)
    private String loanNo;

    /** 还款利息 */
    @Column(name = "int_amt", precision = 17, scale = 6)
    private BigDecimal intAmt;

    /** 还款罚息 */
    @Column(name = "oint_amt", precision = 17, scale = 6)
    private BigDecimal ointAmt;

    /** 担保费1 */
    @Column(name = "fee1_amt", precision = 17, scale = 6)
    private BigDecimal fee1Amt;

    /** 担保费2 */
    @Column(name = "fee2_amt", precision = 17, scale = 6)
    private BigDecimal fee2Amt;

    /** 违约金 */
    @Column(name = "fee3_amt", precision = 17, scale = 6)
    private BigDecimal fee3Amt;

    /** 提前结清手续费 */
    @Column(name = "fee4_amt", precision = 17, scale = 6)
    private BigDecimal fee4Amt;

    /** 贷后逾期管理费 */
    @Column(name = "fee5_amt", precision = 17, scale = 6)
    private BigDecimal fee5Amt;

    /** 催费 */
    @Column(name = "fee6_amt", precision = 17, scale = 6)
    private BigDecimal fee6Amt;

    /** 还款本金 */
    @Column(name = "prin_amt", precision = 17, scale = 6)
    private BigDecimal prinAmt;

    /** 还款期次 */
    @Column(name = "term", length = 64)
    private String term;

    /** 创建时间 */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}