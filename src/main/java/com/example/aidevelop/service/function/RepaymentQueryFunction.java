package com.example.aidevelop.service.function;

import com.example.aidevelop.service.business.RepaymentQueryService;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Description("查询用户的还款记录，支持按用户编号、状态等条件查询")
public class RepaymentQueryFunction implements AiToolProvider {

    private final RepaymentQueryService repaymentQueryService;

    public RepaymentQueryFunction(RepaymentQueryService repaymentQueryService) {
        this.repaymentQueryService = repaymentQueryService;
    }

    @Tool(name = "repaymentQueryFunction", description = "查询用户还款记录，支持按 userNo 和 status 过滤")
    public RepaymentQueryService.Response queryRepaymentRecords(Request request) {
        return repaymentQueryService.queryRepaymentRecords(request.userNo(), request.status());
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
        List<RepaymentInfo> records
    ) {
        public String getSummary() {
            return String.format("用户 %s 共有 %d 条还款记录", userNo, totalCount);
        }
    }

    /**
     * 还款信息
     */
    public record RepaymentInfo(
        String bizSerial,
        String userNo,
        String loanNo,
        java.math.BigDecimal totalAmt,
        String repayType,
        String status,
        java.time.LocalDateTime repaySuccessTime
    ) {
        public String getDescription() {
            return String.format("流水号: %s, 借款号: %s, 还款金额: %.2f元, 类型: %s, 状态: %s",
                bizSerial, loanNo, totalAmt, repayType, status);
        }
    }
}
