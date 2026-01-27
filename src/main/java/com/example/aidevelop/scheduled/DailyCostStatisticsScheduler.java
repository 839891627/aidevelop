package com.example.aidevelop.scheduled;

import com.example.aidevelop.repository.AiCallLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务：每日成本统计
 * 每天凌晨1点执行，统计昨天的AI调用成本数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyCostStatisticsScheduler {

    private final AiCallLogRepository aiCallLogRepository;

    /**
     * 每天凌晨1点执行统计
     * cron = "0 0 1 * * ?"
     * 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void calculateDailyStatistics() {
        log.info("开始执行每日成本统计任务...");

        try {
            // 统计昨天的数据
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDateTime start = yesterday.atStartOfDay();
            LocalDateTime end = yesterday.plusDays(1).atStartOfDay();

            log.info("统计时间范围: {} 到 {}", start, end);

            // 获取统计数据
            List<Object[]> modelStats = aiCallLogRepository.getModelUsageStats(start, end);
            Long totalCalls = aiCallLogRepository.countTotalCalls(start, end);
            Long successCalls = aiCallLogRepository.countSuccessfulCalls(start, end);

            log.info("昨日总调用次数: {}, 成功次数: {}", totalCalls, successCalls);

            // 计算成功率
            BigDecimal successRate = totalCalls != null && totalCalls > 0
                ? BigDecimal.valueOf(successCalls)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalCalls), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            // 计算平均响应时间
            Long avgLatency = calculateAverageLatency(start, end);

            // 输出统计结果（可以保存到统计表或发送告警）
            for (Object[] stat : modelStats) {
                String modelName = (String) stat[0];
                String provider = (String) stat[1];
                Long callCount = (Long) stat[2];
                Long totalTokens = (Long) stat[3];
                BigDecimal totalCost = (BigDecimal) stat[4];

                log.info("模型统计: {} | {} | 调用: {} 次 | Token: {} | 成本: ¥{}",
                    provider, modelName, callCount, totalTokens, totalCost);
            }

            log.info("成功率: {}%, 平均响应时间: {} ms", successRate, avgLatency);

            // TODO: 可以将统计结果保存到 ai_daily_cost_stats 表
            // 或者发送邮件/钉钉/企微通知

            log.info("每日成本统计任务完成");

        } catch (Exception e) {
            log.error("每日成本统计任务执行失败", e);
        }
    }

    /**
     * 计算平均响应时间
     */
    private Long calculateAverageLatency(LocalDateTime start, LocalDateTime end) {
        try {
            // 从数据库查询平均响应时间
            // 如果需要，可以在 AiCallLogRepository 中添加相应的方法
            return 0L; // 占位符
        } catch (Exception e) {
            log.warn("计算平均响应时间失败: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 成本预警检查
     * 每10分钟检查一次今日成本是否超过阈值
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void checkCostThreshold() {
        try {
            LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime end = LocalDateTime.now();

            BigDecimal todayCost = aiCallLogRepository.sumCostBetween(start, end);
            BigDecimal threshold = new BigDecimal("100"); // 设置阈值：¥100

            if (todayCost != null && todayCost.compareTo(threshold) > 0) {
                log.warn("⚠️ 成本预警：今日成本已达到 ¥{}，超过阈值 ¥{}", todayCost, threshold);

                // TODO: 发送告警通知
                // - 邮件
                // - 钉钉/企微
                // - 短信
            }

        } catch (Exception e) {
            log.error("成本预警检查失败", e);
        }
    }
}
