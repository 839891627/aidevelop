package com.example.aidevelop.service.cost;

import com.example.aidevelop.model.entity.AiCallLog;
import com.example.aidevelop.repository.AiCallLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 成本统计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCostStatisticsService {

    private final AiCallLogRepository aiCallLogRepository;

    /**
     * 获取今日统计
     */
    public CostStats getTodayStats() {
        LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime end = LocalDateTime.now();
        return getStats(start, end);
    }

    /**
     * 获取本周统计
     */
    public CostStats getWeekStats() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        return getStats(start, end);
    }

    /**
     * 获取本月统计
     */
    public CostStats getMonthStats() {
        LocalDateTime start = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime end = LocalDateTime.now();
        return getStats(start, end);
    }

    /**
     * 获取指定时间范围的统计
     */
    public CostStats getStats(LocalDateTime start, LocalDateTime end) {
        log.info("查询成本统计: {} 到 {}", start, end);

        BigDecimal totalCost = aiCallLogRepository.sumCostBetween(start, end);
        Long totalCalls = aiCallLogRepository.countTotalCalls(start, end);
        Long successCalls = aiCallLogRepository.countSuccessfulCalls(start, end);

        // 计算成功率
        BigDecimal successRate = totalCalls > 0
            ? BigDecimal.valueOf(successCalls)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalCalls), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // 获取模型使用统计
        List<ModelUsage> modelUsages = getModelUsageStats(start, end);

        // 获取每日成本趋势
        List<DailyCost> dailyCosts = getDailyCostStats(start, end);

        return new CostStats(
            start, end, totalCost != null ? totalCost : BigDecimal.ZERO,
            totalCalls != null ? totalCalls : 0,
            successCalls != null ? successCalls : 0,
            successRate,
            modelUsages,
            dailyCosts
        );
    }

    /**
     * 获取模型使用统计
     */
    private List<ModelUsage> getModelUsageStats(LocalDateTime start, LocalDateTime end) {
        List<Object[]> results = aiCallLogRepository.getModelUsageStats(start, end);
        List<ModelUsage> usages = new ArrayList<>();

        for (Object[] row : results) {
            String modelName = (String) row[0];
            String provider = (String) row[1];
            Long callCount = row[2] != null ? (Long) row[2] : 0L;
            Long totalTokens = row[3] != null ? (Long) row[3] : 0L;
            BigDecimal totalCost = row[4] != null ? (BigDecimal) row[4] : BigDecimal.ZERO;

            usages.add(new ModelUsage(modelName, provider, callCount, totalTokens, totalCost));
        }

        return usages;
    }

    /**
     * 获取每日成本统计
     */
    private List<DailyCost> getDailyCostStats(LocalDateTime start, LocalDateTime end) {
        List<Object[]> results = aiCallLogRepository.getDailyCostStats(start, end);
        List<DailyCost> dailyCosts = new ArrayList<>();

        for (Object[] row : results) {
            java.sql.Date date = (java.sql.Date) row[0];
            BigDecimal dailyCost = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            Long callCount = row[2] != null ? (Long) row[2] : 0L;

            dailyCosts.add(new DailyCost(date.toLocalDate(), dailyCost, callCount));
        }

        return dailyCosts;
    }

    /**
     * 成本统计数据
     */
    public record CostStats(
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal totalCost,         // 总成本
        long totalCalls,              // 总调用次数
        long successCalls,            // 成功次数
        BigDecimal successRate,       // 成功率（%）
        List<ModelUsage> modelUsages, // 模型使用统计
        List<DailyCost> dailyCosts    // 每日成本
    ) {}

    /**
     * 模型使用统计
     */
    public record ModelUsage(
        String modelName,       // 模型名称
        String provider,        // 提供商
        long callCount,         // 调用次数
        long totalTokens,       // 总token数
        BigDecimal totalCost    // 总成本
    ) {}

    /**
     * 每日成本统计
     */
    public record DailyCost(
        java.time.LocalDate date,     // 日期
        BigDecimal cost,              // 当日成本
        long callCount                // 当日调用次数
    ) {}
}
