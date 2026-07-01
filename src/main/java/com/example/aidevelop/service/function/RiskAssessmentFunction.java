package com.example.aidevelop.service.function;

import com.example.aidevelop.service.business.RiskAssessmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@Description("评估用户的借款风险，分析逾期情况和还款能力")
public class RiskAssessmentFunction implements AiToolProvider {

    private final RiskAssessmentService riskAssessmentService;

    public RiskAssessmentFunction(RiskAssessmentService riskAssessmentService) {
        this.riskAssessmentService = riskAssessmentService;
    }

    @Tool(name = "riskAssessmentFunction", description = "评估用户借款风险，返回风险等级和建议")
    public RiskAssessmentService.Response assessRisk(Request request) {
        return riskAssessmentService.assessRisk(request.userNo());
    }

    /**
     * 请求参数
     */
    public record Request(
        String userNo
    ) {}

    /**
     * 响应结果
     */
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

    /**
     * 风险等级枚举
     */
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
