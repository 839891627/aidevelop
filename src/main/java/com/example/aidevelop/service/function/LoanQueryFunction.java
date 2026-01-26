package com.example.aidevelop.service.function;

import com.example.aidevelop.model.entity.FundLoan;
import com.example.aidevelop.repository.FundLoanRepository;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * 借款查询Function - 用于Function Calling
 *
 * 用途：查询金融借款系统的借款信息
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Description("查询借款信息，支持根据业务流水号、三方借据号、合同号或客户编号查询借款")
public class LoanQueryFunction implements Function<LoanQueryFunction.Request, LoanQueryFunction.Response> {

    private final FundLoanRepository loanRepository;

    /**
     * 函数请求参数
     */
    public record Request(
        @JsonProperty(value = "bizSerial", required = false)
        @Description("业务流水号（可选）")
        String bizSerial,

        @JsonProperty(value = "thirdLoanNo", required = false)
        @Description("三方借据号（可选）")
        String thirdLoanNo,

        @JsonProperty(value = "contractNumber", required = false)
        @Description("合同号（可选）")
        String contractNumber,

        @JsonProperty(value = "custNo", required = false)
        @Description("客户编号（可选）- 如果提供，查询该客户的所有借款记录")
        String custNo
    ) {
        @JsonCreator
        public Request {}
    }

    /**
     * 函数响应结果
     */
    public record Response(
        @JsonProperty("success")
        boolean success,

        @JsonProperty("message")
        String message,

        @JsonProperty("loan")
        LoanDetail loan,

        @JsonProperty("loans")
        java.util.List<LoanDetail> loans,

        @JsonProperty("summary")
        LoanSummary summary
    ) {
        /**
         * 借款详情
         */
        public record LoanDetail(
            @JsonProperty("bizSerial")
            String bizSerial,

            @JsonProperty("thirdLoanNo")
            String thirdLoanNo,

            @JsonProperty("contractNumber")
            String contractNumber,

            @JsonProperty("custNo")
            String custNo,

            @JsonProperty("productCode")
            String productCode,

            @JsonProperty("loanAmt")
            String loanAmt,

            @JsonProperty("status")
            String status,

            @JsonProperty("statusText")
            String statusText,

            @JsonProperty("fundStatus")
            String fundStatus,

            @JsonProperty("billStatus")
            String billStatus,

            @JsonProperty("term")
            Integer term,

            @JsonProperty("feeRate")
            String feeRate,

            @JsonProperty("loanSuccessTime")
            String loanSuccessTime,

            @JsonProperty("billRepayTime")
            String billRepayTime
        ) {
            public static LoanDetail fromEntity(FundLoan loan) {
                return new LoanDetail(
                    loan.getBizSerial(),
                    loan.getThirdLoanNo(),
                    loan.getContractNumber(),
                    loan.getCustNo(),
                    loan.getProductCode(),
                    loan.getLoanAmt() != null ? loan.getLoanAmt().toString() : "0",
                    loan.getStatus(),
                    getStatusText(loan.getStatus()),
                    loan.getFundStatus(),
                    loan.getBillStatus(),
                    loan.getTerm(),
                    loan.getFeeRate() != null ? loan.getFeeRate().toString() : "0",
                    loan.getLoanSuccessTime() != null
                        ? loan.getLoanSuccessTime().toString()
                        : "未放款",
                    loan.getBillRepayTime() != null
                        ? loan.getBillRepayTime().toString()
                        : "未结清"
                );
            }

            private static String getStatusText(String status) {
                return switch (status) {
                    case "INIT" -> "创建";
                    case "SUCCESS" -> "成功";
                    case "FAIL" -> "失败";
                    case "PENDING" -> "借款中";
                    default -> status;
                };
            }
        }

        /**
         * 借款汇总信息
         */
        public record LoanSummary(
            @JsonProperty("totalCount")
            int totalCount,

            @JsonProperty("totalAmount")
            String totalAmount,

            @JsonProperty("successCount")
            int successCount,

            @JsonProperty("pendingCount")
            int pendingCount
        ) {
            public LoanSummary(int totalCount, String totalAmount, int successCount, int pendingCount) {
                this.totalCount = totalCount;
                this.totalAmount = totalAmount;
                this.successCount = successCount;
                this.pendingCount = pendingCount;
            }
        }
    }

    @Override
    public Response apply(Request request) {
        log.info("执行借款查询函数：bizSerial={}, thirdLoanNo={}, contractNumber={}, custNo={}",
            request.bizSerial, request.thirdLoanNo, request.contractNumber, request.custNo);

        try {
            // 1. 根据业务流水号查询（优先级最高）
            if (request.bizSerial != null && !request.bizSerial.isEmpty()) {
                FundLoan loan = loanRepository.findByBizSerial(request.bizSerial);
                if (loan != null) {
                    log.info("找到借款记录：bizSerial={}", loan.getBizSerial());
                    return new Response(
                        true,
                        "查询成功",
                        Response.LoanDetail.fromEntity(loan),
                        null,
                        null
                    );
                }
                return new Response(false, "未找到业务流水号：" + request.bizSerial, null, null, null);
            }

            // 2. 根据三方借据号查询
            if (request.thirdLoanNo != null && !request.thirdLoanNo.isEmpty()) {
                FundLoan loan = loanRepository.findByThirdLoanNo(request.thirdLoanNo);
                if (loan != null) {
                    log.info("找到借款记录：thirdLoanNo={}", loan.getThirdLoanNo());
                    return new Response(
                        true,
                        "查询成功",
                        Response.LoanDetail.fromEntity(loan),
                        null,
                        null
                    );
                }
                return new Response(false, "未找到三方借据号：" + request.thirdLoanNo, null, null, null);
            }

            // 3. 根据合同号查询
            if (request.contractNumber != null && !request.contractNumber.isEmpty()) {
                FundLoan loan = loanRepository.findByContractNumber(request.contractNumber);
                if (loan != null) {
                    log.info("找到借款记录：contractNumber={}", loan.getContractNumber());
                    return new Response(
                        true,
                        "查询成功",
                        Response.LoanDetail.fromEntity(loan),
                        null,
                        null
                    );
                }
                return new Response(false, "未找到合同号：" + request.contractNumber, null, null, null);
            }

            // 4. 根据客户编号查询所有借款
            if (request.custNo != null && !request.custNo.isEmpty()) {
                var loans = loanRepository.findByCustNoOrderByCreateTimeDesc(request.custNo);
                log.info("找到客户{}的{}条借款记录", request.custNo, loans.size());

                if (loans.isEmpty()) {
                    return new Response(false, "客户" + request.custNo + "没有借款记录", null, null, null);
                }

                // 计算汇总信息
                int successCount = (int) loans.stream()
                    .filter(l -> "SUCCESS".equals(l.getStatus()))
                    .count();
                int pendingCount = (int) loans.stream()
                    .filter(l -> "PENDING".equals(l.getStatus()))
                    .count();

                var totalAmount = loanRepository.sumLoanAmtByCustNo(request.custNo);

                Response.LoanSummary summary = new Response.LoanSummary(
                    loans.size(),
                    totalAmount.toString(),
                    successCount,
                    pendingCount
                );

                return new Response(
                    true,
                    "查询成功，找到" + loans.size() + "条借款记录",
                    null,
                    loans.stream()
                        .map(Response.LoanDetail::fromEntity)
                        .toList(),
                    summary
                );
            }

            // 都没有提供
            log.warn("查询参数为空");
            return new Response(false, "请提供查询条件（业务流水号/三方借据号/合同号/客户编号）", null, null, null);

        } catch (Exception e) {
            log.error("查询借款失败", e);
            return new Response(false, "查询失败：" + e.getMessage(), null, null, null);
        }
    }
}
