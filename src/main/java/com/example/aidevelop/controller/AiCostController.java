package com.example.aidevelop.controller;

import com.example.aidevelop.service.cost.AiCostStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * AI 成本统计 API
 */
@RestController
@RequestMapping("/api/cost")
@RequiredArgsConstructor
@Tag(name = "成本统计", description = "AI 调用成本统计相关 API")
public class AiCostController {

    private final AiCostStatisticsService statisticsService;

    /**
     * 获取今日成本统计
     */
    @GetMapping("/today")
    @Operation(summary = "今日成本统计", description = "获取今天的AI调用成本统计")
    public AiCostStatisticsService.CostStats getTodayStats() {
        return statisticsService.getTodayStats();
    }

    /**
     * 获取本周成本统计
     */
    @GetMapping("/week")
    @Operation(summary = "本周成本统计", description = "获取本周的AI调用成本统计")
    public AiCostStatisticsService.CostStats getWeekStats() {
        return statisticsService.getWeekStats();
    }

    /**
     * 获取本月成本统计
     */
    @GetMapping("/month")
    @Operation(summary = "本月成本统计", description = "获取本月的AI调用成本统计")
    public AiCostStatisticsService.CostStats getMonthStats() {
        return statisticsService.getMonthStats();
    }

    /**
     * 获取指定时间范围的成本统计
     */
    @GetMapping("/range")
    @Operation(summary = "时间范围成本统计", description = "获取指定时间范围的AI调用成本统计")
    public AiCostStatisticsService.CostStats getRangeStats(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return statisticsService.getStats(start, end);
    }
}
