package com.example.aidevelop.service.function;

import com.example.aidevelop.service.business.LoanQueryService;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Description("查询用户的借款记录，支持按用户编号、状态等条件查询")
public class LoanQueryFunction implements AiToolProvider {

    private final LoanQueryService loanQueryService;

    public LoanQueryFunction(LoanQueryService loanQueryService) {
        this.loanQueryService = loanQueryService;
    }

    @Tool(name = "queryLoanByBizSerial", description = "通过订单编号查询记录")
    public LoanQueryService.Response queryLoanByBizSerial(BizSerialRequest request) {
        return loanQueryService.queryByBizSerial(request.bizSerial());
    }

    @Tool(name = "loanQueryFunction", description = "查询用户借款记录，支持按 userNo 和 status 过滤")
    public LoanQueryService.Response queryLoanRecords(UserStatusRequest request) {
        return loanQueryService.queryLoanRecords(request.userNo(), request.status());
    }

    /**
     * 请求参数
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BizSerialRequest(
        String bizSerial
    ) {}

    /**
     * 请求参数
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserStatusRequest(
        String userNo,
        String status
    ) {}

    /**
     * 响应结果
     */
    public record Response(
        String userNo,
        int totalCount,
        List<LoanInfo> loans
    ) {
        public String getSummary() {
            return String.format("用户 %s 共有 %d 条借款记录", userNo, totalCount);
        }
    }

    /**
     * 借款信息
     */
    public record LoanInfo(
        String bizSerial,
        String userNo,
        String productCode,
        java.math.BigDecimal loanAmt,
        java.math.BigDecimal feeRate,
        String status,
        java.time.LocalDateTime loanSuccessTime
    ) {
        public String getDescription() {
            return String.format("流水号: %s, 产品: %s, 金额: %.2f元, 利率: %.2f%%, 状态: %s",
                bizSerial, productCode, loanAmt, feeRate.multiply(java.math.BigDecimal.valueOf(100)), status);
        }
    }
}
