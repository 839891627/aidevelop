package com.example.aidevelop.service.business;

import com.example.aidevelop.model.entity.RepaymentRecord;
import com.example.aidevelop.repository.RepaymentRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepaymentQueryService {

    private final RepaymentRecordRepository repaymentRecordRepository;

    public Response queryRepaymentRecords(String userNo, String status) {
        log.info("执行还款查询: userNo={}, status={}", userNo, status);
        List<RepaymentRecord> records;
        if (status != null && !status.isEmpty()) {
            records = repaymentRecordRepository.findByUserNo(userNo).stream()
                .filter(record -> status.equals(record.getStatus()))
                .toList();
        } else {
            records = repaymentRecordRepository.findByUserNo(userNo);
        }

        log.info("查询到 {} 条还款记录", records.size());
        return new Response(
            userNo,
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

    public record Response(
        String userNo,
        int totalCount,
        List<RepaymentInfo> records
    ) {
        public String getSummary() {
            return String.format("用户 %s 共有 %d 条还款记录", userNo, totalCount);
        }
    }

    public record RepaymentInfo(
        String bizSerial,
        String userNo,
        String loanNo,
        BigDecimal totalAmt,
        String repayType,
        String status,
        LocalDateTime repaySuccessTime
    ) {
        public String getDescription() {
            return String.format("流水号: %s, 借款号: %s, 还款金额: %.2f元, 类型: %s, 状态: %s",
                bizSerial, loanNo, totalAmt, repayType, status);
        }
    }
}
