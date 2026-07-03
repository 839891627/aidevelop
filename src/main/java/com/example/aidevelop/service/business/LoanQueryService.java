package com.example.aidevelop.service.business;

import com.example.aidevelop.model.entity.Loan;
import com.example.aidevelop.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanQueryService {

    private final LoanRepository loanRepository;

    public Response queryByBizSerial(String bizSerial) {
        log.info("通过订单编号查询记录: bizSerial={}", bizSerial);
        Optional<Loan> loan = loanRepository.findByBizSerial(bizSerial);
        List<Loan> loans = loan.map(List::of).orElseGet(List::of);
        return toResponse(loan.map(Loan::getUserNo).orElse(null), loans);
    }

    public Response queryLoanRecords(String userNo, String status) {
        log.info("执行借款查询: userNo={}, status={}", userNo, status);
        List<Loan> loans;
        if (status != null && !status.isEmpty()) {
            loans = loanRepository.findByUserNo(userNo).stream()
                .filter(loan -> status.equals(loan.getStatus()))
                .toList();
        } else {
            loans = loanRepository.findByUserNo(userNo);
        }
        return toResponse(userNo, loans);
    }

    private Response toResponse(String userNo, List<Loan> loans) {
        log.info("查询到 {} 条借款记录", loans.size());
        return new Response(
            userNo,
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
    }

    public record Response(
        String userNo,
        int totalCount,
        List<LoanInfo> loans
    ) {
        public String getSummary() {
            return String.format("用户 %s 共有 %d 条借款记录", userNo, totalCount);
        }
    }

    public record LoanInfo(
        String bizSerial,
        String userNo,
        String productCode,
        BigDecimal loanAmt,
        BigDecimal feeRate,
        String status,
        LocalDateTime loanSuccessTime
    ) {
        public String getDescription() {
            return String.format("流水号: %s, 产品: %s, 金额: %.2f元, 利率: %.2f%%, 状态: %s",
                bizSerial, productCode, loanAmt, feeRate.multiply(BigDecimal.valueOf(100)), status);
        }
    }
}
