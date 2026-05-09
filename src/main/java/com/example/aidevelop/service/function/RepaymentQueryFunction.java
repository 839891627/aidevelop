package com.example.aidevelop.service.function;

import com.example.aidevelop.config.AiFunction;
import com.example.aidevelop.model.entity.RepaymentRecord;
import com.example.aidevelop.repository.RepaymentRecordRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Slf4j
@Component
@AiFunction
@Description("查询用户的还款记录，支持按用户编号、状态等条件查询")
public class RepaymentQueryFunction implements Function<RepaymentQueryFunction.Request, RepaymentQueryFunction.Response> {

    private final RepaymentRecordRepository repaymentRecordRepository;

    public RepaymentQueryFunction(RepaymentRecordRepository repaymentRecordRepository) {
        this.repaymentRecordRepository = repaymentRecordRepository;
    }

    @Override
    public Response apply(Request request) {
        log.info("执行还款查询: userNo={}, status={}", request.userNo(), request.status());

        List<RepaymentRecord> records;
        if (request.status() != null && !request.status().isEmpty()) {
            // TODO: 需要在 RepaymentRecordRepository 中添加 findByUserNoAndStatus 方法
            records = repaymentRecordRepository.findByUserNo(request.userNo()).stream()
                .filter(record -> request.status().equals(record.getStatus()))
                .toList();
        } else {
            records = repaymentRecordRepository.findByUserNo(request.userNo());
        }

        log.info("查询到 {} 条还款记录", records.size());

        return new Response(
            request.userNo(),
            records.size(),
            records.stream().map(record -> new RepaymentInfo(
                record.getBizSerial(),
                record.getUserNo(),
                record.getLoanNo(),
                record.getTotalAmt(),
                record.getRepayType(),
                record.getStatus(),
                record.getRepaySuccessTime()
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
