package com.example.aidevelop.service.function;

import com.example.aidevelop.model.entity.Loan;
import com.example.aidevelop.repository.LoanRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * 借款查询功能（演示版）
 * 用于 AI Function Calling，让 AI 能够查询用户的借款信息
 */
@Slf4j
@Component
@Description("查询用户的借款记录，支持按用户编号、状态等条件查询")
public class LoanQueryFunction implements Function<LoanQueryFunction.Request, LoanQueryFunction.Response> {

    private final LoanRepository loanRepository;

    public LoanQueryFunction(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @Override
    public Response apply(Request request) {
        log.info("执行借款查询: userNo={}, status={}", request.userNo(), request.status());

        List<Loan> loans;
        if (request.status() != null && !request.status().isEmpty()) {
            // TODO: 需要在 LoanRepository 中添加 findByUserNoAndStatus 方法
            loans = loanRepository.findByUserNo(request.userNo()).stream()
                .filter(loan -> request.status().equals(loan.getStatus()))
                .toList();
        } else {
            loans = loanRepository.findByUserNo(request.userNo());
        }

        log.info("查询到 {} 条借款记录", loans.size());

        return new Response(
            request.userNo(),
            loans.size(),
            loans.stream().map(loan -> new LoanInfo(
                loan.getBizSerial(),
                loan.getUserNo(),
                loan.getProductCode(),
                loan.getLoanAmt(),
                loan.getFeeRate(),
                loan.getStatus(),
                loan.getLoanSuccessTime()
            )).toList()
        );
    }

    /**
     * 请求参数
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Request(
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
