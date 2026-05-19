package com.example.aidevelop.service.function;

import com.example.aidevelop.model.entity.Loan;
import com.example.aidevelop.repository.LoanRepository;
import com.example.aidevelop.service.cache.AiCacheService;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@Description("查询用户的借款记录，支持按用户编号、状态等条件查询")
public class LoanQueryFunction implements AiToolProvider {

    private final LoanRepository loanRepository;
    private final AiCacheService aiCacheService;

    public LoanQueryFunction(LoanRepository loanRepository, AiCacheService aiCacheService) {
        this.loanRepository = loanRepository;
        this.aiCacheService = aiCacheService;
    }

    @Tool(name = "queryLoanByBizSerial", description = "通过订单编号查询记录")
    public Response queryLoanByBizSerial(BizSerialRequest request) {
        log.info("通过订单编号查询记录: bizSerial={}", request.bizSerial());
        String cacheKey = buildCacheKey("queryLoanByBizSerial", "bizSerial", request.bizSerial());
        var cached = aiCacheService.<Response>get(AiCacheService.TOOL_CALL, cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        Optional<Loan> loan = loanRepository.findByBizSerial(request.bizSerial());

        // 1. 查询单条记录
        List<Loan> loans = loan.isPresent() 
            ? List.of(loan.get()) 
            : List.of();

        log.info("查询到 {} 条借款记录", loans.size());

        Response response = new Response(
                loan.map(Loan::getUserNo).orElse(null),
                loans.size(),
                loans.stream().map(item -> new LoanInfo(
                        item.getBizSerial(),
                        item.getUserNo(),
                        item.getProductCode(),
                        item.getLoanAmt(),
                        item.getFeeRate(),
                        item.getStatus(),
                        item.getLoanSuccessTime()
                )).toList()
        );
        aiCacheService.put(AiCacheService.TOOL_CALL, cacheKey, response);
        return response;
    }
    @Tool(name = "loanQueryFunction", description = "查询用户借款记录，支持按 userNo 和 status 过滤")
    public Response queryLoanRecords(UserStatusRequest request) {
        log.info("执行借款查询: userNo={}, status={}", request.userNo(), request.status());
        String cacheKey = buildCacheKey("queryLoanRecords",
                "userNo", request.userNo(),
                "status", request.status());
        var cached = aiCacheService.<Response>get(AiCacheService.TOOL_CALL, cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

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

        Response response = new Response(
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
        aiCacheService.put(AiCacheService.TOOL_CALL, cacheKey, response);
        return response;
    }

    private String buildCacheKey(String method, String... parts) {
        StringBuilder builder = new StringBuilder("tool=loan|method=").append(method);
        for (int i = 0; i + 1 < parts.length; i += 2) {
            builder.append('|').append(parts[i]).append('=').append(normalize(parts[i + 1]));
        }
        return builder.toString();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
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
