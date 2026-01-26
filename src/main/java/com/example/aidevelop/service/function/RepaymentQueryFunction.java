package com.example.aidevelop.service.function;

import com.example.aidevelop.model.entity.FundRepayRecord;
import com.example.aidevelop.repository.FundRepayRecordRepository;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.function.Function;

/**
 * 还款查询Function - 用于Function Calling
 *
 * 用途：查询金融借款系统的还款记录
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Description("查询还款记录，支持根据业务流水号、资方还款申请流水号、借款编号、客户编号等条件查询")
public class RepaymentQueryFunction implements Function<RepaymentQueryFunction.Request, RepaymentQueryFunction.Response> {

    private final FundRepayRecordRepository repayRecordRepository;

    /**
     * 函数请求参数
     */
    public record Request(
        @JsonProperty(value = "bizSerial", required = false)
        @Description("业务流水号（可选）")
        String bizSerial,

        @JsonProperty(value = "fundReqNo", required = false)
        @Description("资方还款申请流水号（可选）")
        String fundReqNo,

        @JsonProperty(value = "thirdRespNo", required = false)
        @Description("外部借款流水号/三方响应号（可选）")
        String thirdRespNo,

        @JsonProperty(value = "loanNo", required = false)
        @Description("借款编号（可选）- 查询该借款的所有还款记录")
        String loanNo,

        @JsonProperty(value = "thirdLoanNo", required = false)
        @Description("三方借据号（可选）- 查询该借据的所有还款记录")
        String thirdLoanNo,

        @JsonProperty(value = "custNo", required = false)
        @Description("客户编号（可选）- 如果提供，查询该客户的所有还款记录")
        String custNo,

        @JsonProperty(value = "status", required = false)
        @Description("还款状态（可选）：INIT-初始, PENDING-还款中, SUCCESS-成功, FAIL-失败")
        String status
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

        @JsonProperty("repayment")
        RepaymentDetail repayment,

        @JsonProperty("repayments")
        java.util.List<RepaymentDetail> repayments,

        @JsonProperty("summary")
        RepaymentSummary summary
    ) {
        /**
         * 还款详情
         */
        public record RepaymentDetail(
            @JsonProperty("bizSerial")
            String bizSerial,

            @JsonProperty("loanNo")
            String loanNo,

            @JsonProperty("thirdLoanNo")
            String thirdLoanNo,

            @JsonProperty("custNo")
            String custNo,

            @JsonProperty("totalAmt")
            String totalAmt,

            @JsonProperty("actTotalAmt")
            String actTotalAmt,

            @JsonProperty("status")
            String status,

            @JsonProperty("statusText")
            String statusText,

            @JsonProperty("repayType")
            String repayType,

            @JsonProperty("repayTypeText")
            String repayTypeText,

            @JsonProperty("repayMethod")
            String repayMethod,

            @JsonProperty("repayMethodText")
            String repayMethodText,

            @JsonProperty("repayAcceptTime")
            String repayAcceptTime,

            @JsonProperty("repaySuccessTime")
            String repaySuccessTime,

            @JsonProperty("term")
            String term,

            @JsonProperty("prinAmt")
            String prinAmt,

            @JsonProperty("intAmt")
            String intAmt,

            @JsonProperty("respMsg")
            String respMsg
        ) {
            public static RepaymentDetail fromEntity(FundRepayRecord record) {
                return new RepaymentDetail(
                    record.getBizSerial(),
                    record.getLoanNo(),
                    record.getThirdLoanNo(),
                    record.getCustNo(),
                    formatDecimal(record.getTotalAmt()),
                    formatDecimal(record.getActTotalAmt()),
                    record.getStatus(),
                    getStatusText(record.getStatus()),
                    record.getRepayType(),
                    getRepayTypeText(record.getRepayType()),
                    record.getRepayMethod(),
                    getRepayMethodText(record.getRepayMethod()),
                    formatDateTime(record.getRepayAcceptTime()),
                    formatDateTime(record.getRepaySuccessTime()),
                    record.getTerm(),
                    formatDecimal(record.getPrinAmt()),
                    formatDecimal(record.getIntAmt()),
                    record.getRespMsg()
                );
            }

            private static String formatDecimal(BigDecimal value) {
                return value != null ? value.setScale(2, BigDecimal.ROUND_HALF_UP).toString() : "0.00";
            }

            private static String formatDateTime(java.time.LocalDateTime dateTime) {
                return dateTime != null ? dateTime.toString() : "未完成";
            }

            private static String getStatusText(String status) {
                return switch (status) {
                    case "INIT" -> "初始";
                    case "PENDING" -> "还款中";
                    case "SUCCESS" -> "成功";
                    case "FAIL" -> "失败";
                    default -> status;
                };
            }

            private static String getRepayTypeText(String repayType) {
                return switch (repayType) {
                    case "AD" -> "提前还款";
                    case "DUE" -> "到期还款";
                    case "OVER" -> "逾期还款";
                    case "SETTLE" -> "提前结清";
                    default -> repayType;
                };
            }

            private static String getRepayMethodText(String repayMethod) {
                return switch (repayMethod) {
                    case "ONLINE" -> "线上扣款";
                    case "OFFLINE" -> "线下还款";
                    default -> repayMethod;
                };
            }
        }

        /**
         * 还款汇总信息
         */
        public record RepaymentSummary(
            @JsonProperty("totalCount")
            int totalCount,

            @JsonProperty("successCount")
            int successCount,

            @JsonProperty("pendingCount")
            int pendingCount,

            @JsonProperty("failCount")
            int failCount,

            @JsonProperty("totalAmount")
            String totalAmount,

            @JsonProperty("totalPrincipal")
            String totalPrincipal
        ) {
            public RepaymentSummary(int totalCount, int successCount, int pendingCount, 
                                    int failCount, String totalAmount, String totalPrincipal) {
                this.totalCount = totalCount;
                this.successCount = successCount;
                this.pendingCount = pendingCount;
                this.failCount = failCount;
                this.totalAmount = totalAmount;
                this.totalPrincipal = totalPrincipal;
            }
        }
    }

    @Override
    public Response apply(Request request) {
        log.info("执行还款查询函数：bizSerial={}, fundReqNo={}, loanNo={}, custNo={}, status={}",
            request.bizSerial, request.fundReqNo, request.loanNo, request.custNo, request.status);

        try {
            // 1. 根据业务流水号查询（优先级最高）
            if (request.bizSerial != null && !request.bizSerial.isEmpty()) {
                FundRepayRecord record = repayRecordRepository.findByBizSerial(request.bizSerial);
                if (record != null) {
                    log.info("找到还款记录：bizSerial={}", record.getBizSerial());
                    return new Response(
                        true,
                        "查询成功",
                        Response.RepaymentDetail.fromEntity(record),
                        null,
                        null
                    );
                }
                return new Response(false, "未找到业务流水号：" + request.bizSerial, null, null, null);
            }

            // 2. 根据资方还款申请流水号查询
            if (request.fundReqNo != null && !request.fundReqNo.isEmpty()) {
                FundRepayRecord record = repayRecordRepository.findByFundReqNo(request.fundReqNo);
                if (record != null) {
                    log.info("找到还款记录：fundReqNo={}", record.getFundReqNo());
                    return new Response(
                        true,
                        "查询成功",
                        Response.RepaymentDetail.fromEntity(record),
                        null,
                        null
                    );
                }
                return new Response(false, "未找到资方还款申请流水号：" + request.fundReqNo, null, null, null);
            }

            // 3. 根据外部借款流水号查询
            if (request.thirdRespNo != null && !request.thirdRespNo.isEmpty()) {
                FundRepayRecord record = repayRecordRepository.findByThirdRespNo(request.thirdRespNo);
                if (record != null) {
                    log.info("找到还款记录：thirdRespNo={}", record.getThirdRespNo());
                    return new Response(
                        true,
                        "查询成功",
                        Response.RepaymentDetail.fromEntity(record),
                        null,
                        null
                    );
                }
                return new Response(false, "未找到外部借款流水号：" + request.thirdRespNo, null, null, null);
            }

            // 4. 根据借款编号查询还款列表
            if (request.loanNo != null && !request.loanNo.isEmpty()) {
                java.util.List<FundRepayRecord> records;
                
                if (request.status != null && !request.status.isEmpty()) {
                    records = repayRecordRepository.findByLoanNoAndStatusOrderByCreateTimeDesc(
                        request.loanNo, request.status);
                } else {
                    records = repayRecordRepository.findByLoanNoOrderByCreateTimeDesc(request.loanNo);
                }
                
                log.info("找到借款{}的{}条还款记录", request.loanNo, records.size());

                if (records.isEmpty()) {
                    return new Response(false, "借款" + request.loanNo + "没有还款记录", null, null, null);
                }

                return buildListResponse(records);
            }

            // 5. 根据三方借据号查询还款列表
            if (request.thirdLoanNo != null && !request.thirdLoanNo.isEmpty()) {
                java.util.List<FundRepayRecord> records = 
                    repayRecordRepository.findByThirdLoanNoOrderByCreateTimeDesc(request.thirdLoanNo);
                
                log.info("找到借据{}的{}条还款记录", request.thirdLoanNo, records.size());

                if (records.isEmpty()) {
                    return new Response(false, "借据" + request.thirdLoanNo + "没有还款记录", null, null, null);
                }

                return buildListResponse(records);
            }

            // 6. 根据客户编号查询还款列表
            if (request.custNo != null && !request.custNo.isEmpty()) {
                java.util.List<FundRepayRecord> records;
                
                if (request.status != null && !request.status.isEmpty()) {
                    records = repayRecordRepository.findByStatusOrderByCreateTimeDesc(request.status)
                        .stream()
                        .filter(r -> request.custNo.equals(r.getCustNo()))
                        .toList();
                } else {
                    records = repayRecordRepository.findByCustNoOrderByCreateTimeDesc(request.custNo);
                }
                
                log.info("找到客户{}的{}条还款记录", request.custNo, records.size());

                if (records.isEmpty()) {
                    return new Response(false, "客户" + request.custNo + "没有还款记录", null, null, null);
                }

                return buildListResponseWithSummary(records, request.custNo);
            }

            // 都没有提供
            log.warn("查询参数为空");
            return new Response(false, "请提供查询条件（业务流水号/资方流水号/借款编号/三方借据号/客户编号）", 
                null, null, null);

        } catch (Exception e) {
            log.error("查询还款记录失败", e);
            return new Response(false, "查询失败：" + e.getMessage(), null, null, null);
        }
    }

    /**
     * 构建列表响应（不含汇总）
     */
    private Response buildListResponse(java.util.List<FundRepayRecord> records) {
        return new Response(
            true,
            "查询成功，找到" + records.size() + "条还款记录",
            null,
            records.stream()
                .map(Response.RepaymentDetail::fromEntity)
                .toList(),
            null
        );
    }

    /**
     * 构建列表响应（含汇总）
     */
    private Response buildListResponseWithSummary(java.util.List<FundRepayRecord> records, String custNo) {
        // 计算汇总信息
        int successCount = (int) records.stream()
            .filter(r -> "SUCCESS".equals(r.getStatus()))
            .count();
        int pendingCount = (int) records.stream()
            .filter(r -> "PENDING".equals(r.getStatus()))
            .count();
        int failCount = (int) records.stream()
            .filter(r -> "FAIL".equals(r.getStatus()))
            .count();

        // 统计总金额和本金
        BigDecimal totalAmount = records.stream()
            .filter(r -> r.getActTotalAmt() != null)
            .map(FundRepayRecord::getActTotalAmt)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPrincipal = records.stream()
            .filter(r -> r.getPrinAmt() != null)
            .map(FundRepayRecord::getPrinAmt)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Response.RepaymentSummary summary = new Response.RepaymentSummary(
            records.size(),
            successCount,
            pendingCount,
            failCount,
            totalAmount.setScale(2, BigDecimal.ROUND_HALF_UP).toString(),
            totalPrincipal.setScale(2, BigDecimal.ROUND_HALF_UP).toString()
        );

        return new Response(
            true,
            "查询成功，找到" + records.size() + "条还款记录",
            null,
            records.stream()
                .map(Response.RepaymentDetail::fromEntity)
                .toList(),
            summary
        );
    }
}