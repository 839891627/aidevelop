package com.example.aidevelop.repository;

import com.example.aidevelop.model.entity.AiCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * AI 调用日志 Repository
 */
@Repository
public interface AiCallLogRepository extends JpaRepository<AiCallLog, Long> {

    /**
     * 查询指定时间范围内的日志
     */
    List<AiCallLog> findByCreatedTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 查询指定会话的所有日志
     */
    List<AiCallLog> findBySessionId(String sessionId);

    /**
     * 统计指定时间范围内的总成本
     */
    @Query("SELECT COALESCE(SUM(a.cost), 0) FROM AiCallLog a WHERE a.createdTime BETWEEN :start AND :end")
    BigDecimal sumCostBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 统计每个模型的使用情况
     */
    @Query("""
        SELECT a.modelName, a.provider, COUNT(a) as callCount,
               SUM(a.totalTokens) as totalTokens, SUM(a.cost) as totalCost
        FROM AiCallLog a
        WHERE a.createdTime BETWEEN :start AND :end
        GROUP BY a.modelName, a.provider
        ORDER BY totalCost DESC
    """)
    List<Object[]> getModelUsageStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 统计每日成本
     */
    @Query("""
        SELECT DATE(a.createdTime) as date, SUM(a.cost) as dailyCost, COUNT(a) as callCount
        FROM AiCallLog a
        WHERE a.createdTime BETWEEN :start AND :end
        GROUP BY DATE(a.createdTime)
        ORDER BY date DESC
    """)
    List<Object[]> getDailyCostStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 统计成功率
     */
    @Query("""
        SELECT COUNT(a) FROM AiCallLog a
        WHERE a.createdTime BETWEEN :start AND :end AND a.status = 'SUCCESS'
    """)
    Long countSuccessfulCalls(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 统计总调用次数
     */
    @Query("SELECT COUNT(a) FROM AiCallLog a WHERE a.createdTime BETWEEN :start AND :end")
    Long countTotalCalls(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
