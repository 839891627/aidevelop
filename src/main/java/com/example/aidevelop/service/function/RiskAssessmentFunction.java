package com.example.aidevelop.service.function;

import com.example.aidevelop.model.entity.FundLoan;
import com.example.aidevelop.model.entity.FundRepayRecord;
import com.example.aidevelop.repository.FundLoanRepository;
import com.example.aidevelop.repository.FundRepayRecordRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 风险评估Function - 用于Function Calling
 * <p>
 * 用途：评估客户风险等级，计算风险评分并给出建议
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Description("评估客户风险等级，计算风险评分并给出建议")
public class RiskAssessmentFunction implements Function<RiskAssessmentFunction.Request, RiskAssessmentFunction.Response> {

    private final FundLoanRepository loanRepository;
    private final FundRepayRecordRepository repayRecordRepository;

    /**
     * 风险因素
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RiskFactor {
        @JsonProperty("name")
        private String name;

        @JsonProperty("value")
        private String value;

        @JsonProperty("status")
        private String status;  // normal/warning/danger
    }

    /**
     * 风险汇总
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RiskSummary {
        @JsonProperty("totalLoanCount")
        private int totalLoanCount;

        @JsonProperty("totalLoanAmount")
        private String totalLoanAmount;

        @JsonProperty("totalRepaymentCount")
        private int totalRepaymentCount;

        @JsonProperty("totalRepaymentAmount")
        private String totalRepaymentAmount;

        @JsonProperty("successRepaymentCount")
        private int successRepaymentCount;

        @JsonProperty("overdueCount")
        private int overdueCount;

        @JsonProperty("overdueRate")
        private String overdueRate;

        @JsonProperty("daysSinceLastRepayment")
        private int daysSinceLastRepayment;
    }

    /**
     * 函数请求参数
     */
    public record Request(
            @JsonProperty("custNo")
            @Description("客户编号（必填）")
            String custNo
    ) {
    }

    /**
     * 函数响应结果
     */
    public record Response(
            @JsonProperty("success")
            boolean success,

            @JsonProperty("message")
            String message,

            @JsonProperty("custNo")
            String custNo,

            @JsonProperty("riskScore")
            int riskScore,              // 0-100，分数越高风险越大

            @JsonProperty("riskLevel")
            String riskLevel,           // 低/中/高

            @JsonProperty("riskLevelText")
            String riskLevelText,

            @JsonProperty("riskFactors")
            List<RiskFactor> riskFactors,

            @JsonProperty("summary")
            RiskSummary summary,

            @JsonProperty("suggestions")
            List<String> suggestions
    ) {
    }

    @Override
    public Response apply(Request request) {
        log.info("执行风险评估：custNo={}", request.custNo);

        try {
            // 1. 查询借款数据
            List<FundLoan> loans = loanRepository.findByCustNoOrderByCreateTimeDesc(request.custNo);

            // 2. 查询还款数据
            List<FundRepayRecord> repayments = repayRecordRepository.findByCustNoOrderByCreateTimeDesc(request.custNo);

            if (loans.isEmpty()) {
                return new Response(
                        false,
                        "客户没有借款记录",
                        request.custNo,
                        0,
                        "无数据",
                        "无数据",
                        List.of(),
                        null,
                        List.of("客户暂无借款记录，无法评估风险")
                );
            }

            // 3. 计算风险指标
            RiskAssessmentResult result = assessRisk(loans, repayments);

            // 4. 生成建议
            List<String> suggestions = generateSuggestions(result);

            return new Response(
                    true,
                    "风险评估完成",
                    request.custNo,
                    result.riskScore,
                    result.riskLevel,
                    getRiskLevelText(result.riskLevel),
                    result.riskFactors,
                    result.summary,
                    suggestions
            );

        } catch (Exception e) {
            log.error("风险评估失败", e);
            return new Response(
                    false,
                    "评估失败：" + e.getMessage(),
                    request.custNo,
                    0,
                    "未知",
                    "未知",
                    List.of(),
                    null,
                    List.of()
            );
        }
    }

    /**
     * 核心风险评估逻辑
     */
    private RiskAssessmentResult assessRisk(List<FundLoan> loans, List<FundRepayRecord> repayments) {
        int score = 0;
        List<RiskFactor> factors = new ArrayList<>();

        // 计算汇总数据
        int totalLoanCount = loans.size();
        java.math.BigDecimal totalLoanAmount = loans.stream()
                .filter(l -> l.getLoanAmt() != null)
                .map(FundLoan::getLoanAmt)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        int totalRepaymentCount = repayments.size();
        int successRepaymentCount = (int) repayments.stream()
                .filter(r -> "SUCCESS".equals(r.getStatus()))
                .count();
        long overdueCount = repayments.stream()
                .filter(r -> "FAIL".equals(r.getStatus()))
                .count();

        java.math.BigDecimal totalRepaymentAmount = repayments.stream()
                .filter(r -> r.getActTotalAmt() != null)
                .map(FundRepayRecord::getActTotalAmt)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        // 因素1：逾期次数
        String overdueStatus = getOverdueStatus(overdueCount);
        factors.add(new RiskFactor("逾期次数", overdueCount + "次", overdueStatus));
        score += overdueCount * 15;

        // 因素2：逾期率
        double overdueRate = totalRepaymentCount > 0 ? (overdueCount * 100.0 / totalRepaymentCount) : 0;
        String overdueRateStatus = getOverdueRateStatus(overdueRate);
        factors.add(new RiskFactor("逾期率", String.format("%.1f%%", overdueRate), overdueRateStatus));
        if (overdueRate > 30) score += 25;
        else if (overdueRate > 20) score += 20;
        else if (overdueRate > 10) score += 10;

        // 因素3：最近还款时间
        int daysSinceLastRepayment = 9999;
        if (!repayments.isEmpty()) {
            LocalDateTime lastRepayment = repayments.get(0).getRepaySuccessTime();
            if (lastRepayment != null) {
                daysSinceLastRepayment = (int) ChronoUnit.DAYS.between(lastRepayment, LocalDateTime.now());
            }
        }
        String lastPaymentStatus = getLastPaymentStatus(daysSinceLastRepayment);
        factors.add(new RiskFactor("距上次还款", daysSinceLastRepayment + "天", lastPaymentStatus));
        if (daysSinceLastRepayment > 60) score += 20;
        else if (daysSinceLastRepayment > 30) score += 10;

        // 因素4：借款集中度（连续失败）
        int consecutiveFailures = calculateConsecutiveFailures(repayments);
        String consecutiveStatus = getConsecutiveStatus(consecutiveFailures);
        factors.add(new RiskFactor("连续失败", consecutiveFailures + "次", consecutiveStatus));
        if (consecutiveFailures >= 3) score += 25;
        else if (consecutiveFailures >= 2) score += 15;

        // 因素5：负债率（借款金额 / 还款金额）
        java.math.BigDecimal debtRate = java.math.BigDecimal.ZERO;
        if (totalLoanAmount.compareTo(java.math.BigDecimal.ZERO) > 0) {
            debtRate = totalRepaymentAmount.multiply(java.math.BigDecimal.valueOf(100))
                    .divide(totalLoanAmount, 2, java.math.BigDecimal.ROUND_HALF_UP);
        }
        String debtRateStatus = getDebtRateStatus(debtRate);
        factors.add(new RiskFactor("还款进度", debtRate + "%", debtRateStatus));
        if (debtRate.compareTo(new java.math.BigDecimal("30")) < 0) score += 10;

        // 确保分数在 0-100 范围内
        score = Math.min(100, Math.max(0, score));

        // 确定风险等级
        String riskLevel = getRiskLevel(score);

        // 构建汇总信息
        RiskSummary summary = new RiskSummary(
                totalLoanCount,
                totalLoanAmount.setScale(2, java.math.BigDecimal.ROUND_HALF_UP).toString(),
                totalRepaymentCount,
                totalRepaymentAmount.setScale(2, java.math.BigDecimal.ROUND_HALF_UP).toString(),
                successRepaymentCount,
                (int) overdueCount,
                String.format("%.1f%%", overdueRate),
                daysSinceLastRepayment
        );

        return new RiskAssessmentResult(score, riskLevel, factors, summary);
    }

    /**
     * 生成建议
     */
    private List<String> generateSuggestions(RiskAssessmentResult result) {
        List<String> suggestions = new ArrayList<>();

        int score = result.riskScore;

        if (score >= 70) {
            // 高风险
            suggestions.add("🔴 暂停新增借款额度");
            suggestions.add("🔴 立即联系客户确认还款计划");
            if (result.overdueCount > 0) {
                suggestions.add("🔴 处理" + result.overdueCount + "笔逾期还款");
            }
            suggestions.add("🟡 审核客户账户状态");
        } else if (score >= 40) {
            // 中风险
            suggestions.add("🟡 降低借款额度");
            suggestions.add("🟢 发送还款提醒");
            if (result.daysSinceLastRepayment > 30) {
                suggestions.add("🟡 关注客户还款情况（已" + result.daysSinceLastRepayment + "天未还款）");
            }
            suggestions.add("🟢 建议缩短还款周期");
        } else {
            // 低风险
            suggestions.add("🟢 维持当前额度");
            suggestions.add("🟢 持续监控还款行为");
            suggestions.add("💡 优质客户，可考虑提高额度");
        }

        return suggestions;
    }

    // ========== 辅助方法 ==========

    private String getOverdueStatus(long count) {
        if (count == 0) return "normal";
        if (count <= 2) return "warning";
        return "danger";
    }

    private String getOverdueRateStatus(double rate) {
        if (rate <= 5) return "normal";
        if (rate <= 20) return "warning";
        return "danger";
    }

    private String getLastPaymentStatus(int days) {
        if (days <= 15) return "normal";
        if (days <= 30) return "warning";
        return "danger";
    }

    private String getConsecutiveStatus(int count) {
        if (count == 0) return "normal";
        if (count <= 1) return "warning";
        return "danger";
    }

    private String getDebtRateStatus(java.math.BigDecimal rate) {
        if (rate.compareTo(new java.math.BigDecimal("50")) >= 0) return "normal";
        if (rate.compareTo(new java.math.BigDecimal("30")) >= 0) return "warning";
        return "danger";
    }

    private String getRiskLevel(int score) {
        if (score >= 70) return "HIGH";
        if (score >= 40) return "MEDIUM";
        return "LOW";
    }

    private String getRiskLevelText(String level) {
        return switch (level) {
            case "HIGH" -> "高风险";
            case "MEDIUM" -> "中风险";
            case "LOW" -> "低风险";
            default -> level;
        };
    }

    private int calculateConsecutiveFailures(List<FundRepayRecord> repayments) {
        int consecutive = 0;
        for (FundRepayRecord record : repayments) {
            if ("FAIL".equals(record.getStatus())) {
                consecutive++;
            } else if (consecutive > 0) {
                break;
            }
        }
        return consecutive;
    }

    /**
     * 风险评估结果内部类
     */
    private static class RiskAssessmentResult {
        int riskScore;
        String riskLevel;
        List<RiskFactor> riskFactors;
        int overdueCount;
        int daysSinceLastRepayment;
        RiskSummary summary;

        RiskAssessmentResult(int riskScore, String riskLevel,
                             List<RiskFactor> riskFactors,
                             RiskSummary summary) {
            this.riskScore = riskScore;
            this.riskLevel = riskLevel;
            this.riskFactors = riskFactors;
            this.summary = summary;
            this.overdueCount = (int) riskFactors.stream()
                    .filter(f -> "逾期次数".equals(f.getName()))
                    .findFirst()
                    .map(f -> Integer.parseInt(f.value.replace("次", "")))
                    .orElse(0);
            this.daysSinceLastRepayment = (int) riskFactors.stream()
                    .filter(f -> "距上次还款".equals(f.getName()))
                    .findFirst()
                    .map(f -> Integer.parseInt(f.value.replace("天", "")))
                    .orElse(0);
        }
    }
}