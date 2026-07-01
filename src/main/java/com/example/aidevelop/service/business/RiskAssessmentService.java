package com.example.aidevelop.service.business;

import com.example.aidevelop.model.entity.Loan;
import com.example.aidevelop.model.entity.RepaymentRecord;
import com.example.aidevelop.repository.LoanRepository;
import com.example.aidevelop.repository.RepaymentRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskAssessmentService {

    private final LoanRepository loanRepository;
    private final RepaymentRecordRepository repaymentRecordRepository;

    public Response assessRisk(String userNo) {
        log.info("执行风险评估: userNo={}", userNo);
        List<Loan> loans = loanRepository.findByUserNo(userNo);
        if (loans.isEmpty()) {
            return new Response(userNo, RiskLevel.UNKNOWN, 0, 0, 0, BigDecimal.ZERO, "用户无借款记录");
        }

        List<RepaymentRecord> repayments = repaymentRecordRepository.findByUserNo(userNo);
        int totalLoans = loans.size();
        int overdueCount = 0;
        int pendingRepaymentCount = 0;
        BigDecimal totalOverdueAmount = BigDecimal.ZERO;

        for (Loan loan : loans) {
            if ("PENDING".equals(loan.getStatus())) {
                pendingRepaymentCount++;
                if (loan.getLoanSuccessTime() != null) {
                    long daysSinceLoan = ChronoUnit.DAYS.between(loan.getLoanSuccessTime(), LocalDateTime.now());
                    if (daysSinceLoan > 30) {
                        overdueCount++;
                        totalOverdueAmount = totalOverdueAmount.add(loan.getLoanAmt());
                    }
                }
            } else if ("FAIL".equals(loan.getStatus())) {
                overdueCount++;
                totalOverdueAmount = totalOverdueAmount.add(loan.getLoanAmt());
            }
        }

        long successRepaymentCount = repayments.stream()
            .filter(r -> "SUCCESS".equals(r.getStatus()))
            .count();
        RiskLevel riskLevel = calculateRiskLevel(overdueCount, totalLoans, successRepaymentCount, pendingRepaymentCount);
        String riskDescription = buildRiskDescription(riskLevel, overdueCount, pendingRepaymentCount, successRepaymentCount, totalOverdueAmount);

        log.info("风险评估完成: userNo={}, level={}, description={}", userNo, riskLevel, riskDescription);
        return new Response(userNo, riskLevel, totalLoans, overdueCount, pendingRepaymentCount, totalOverdueAmount, riskDescription);
    }

    private RiskLevel calculateRiskLevel(int overdueCount, int totalLoans, long successRepaymentCount, int pendingRepaymentCount) {
        if (overdueCount >= 2) {
            return RiskLevel.HIGH;
        } else if (overdueCount == 1 || pendingRepaymentCount >= 2) {
            return RiskLevel.MEDIUM;
        } else if (totalLoans > 0 && successRepaymentCount >= totalLoans) {
            return RiskLevel.LOW;
        } else {
            return RiskLevel.UNKNOWN;
        }
    }

    private String buildRiskDescription(RiskLevel riskLevel, int overdueCount, int pendingRepaymentCount,
                                        long successRepaymentCount, BigDecimal totalOverdueAmount) {
        StringBuilder sb = new StringBuilder();
        sb.append("风险等级：").append(riskLevel.getDescription()).append("。");
        if (overdueCount > 0) {
            sb.append("存在").append(overdueCount).append("笔逾期借款，逾期金额：").append(totalOverdueAmount).append("元。");
        }
        if (pendingRepaymentCount > 0) {
            sb.append("当前有").append(pendingRepaymentCount).append("笔借款未结清。");
        }
        if (successRepaymentCount > 0) {
            sb.append("历史成功还款").append(successRepaymentCount).append("次。");
        }
        sb.append("建议：");
        switch (riskLevel) {
            case HIGH -> sb.append("严格审核，建议降低额度或拒绝借款。");
            case MEDIUM -> sb.append("需要关注还款能力，建议适当控制额度。");
            case LOW -> sb.append("信用良好，可以考虑增加额度。");
            case UNKNOWN -> sb.append("需要更多信息进行评估。");
        }
        return sb.toString();
    }

    public record Response(
        String userNo,
        RiskLevel riskLevel,
        int totalLoans,
        int overdueCount,
        int pendingRepaymentCount,
        BigDecimal totalOverdueAmount,
        String riskDescription
    ) {
        public String getSummary() {
            return String.format("用户 %s 风险等级：%s，总借款：%d笔，逾期：%d笔，未结清：%d笔",
                userNo, riskLevel.getDescription(), totalLoans, overdueCount, pendingRepaymentCount);
        }
    }

    public enum RiskLevel {
        LOW("低风险"),
        MEDIUM("中等风险"),
        HIGH("高风险"),
        UNKNOWN("未知");

        private final String description;

        RiskLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
