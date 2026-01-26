package com.example.aidevelop.repository;

import com.example.aidevelop.model.entity.FundRepayRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 还款记录表 Repository
 */
@Repository
public interface FundRepayRecordRepository extends JpaRepository<FundRepayRecord, Long> {

    /**
     * 根据业务流水号查询
     */
    FundRepayRecord findByBizSerial(String bizSerial);

    /**
     * 根据资方还款申请流水号查询
     */
    FundRepayRecord findByFundReqNo(String fundReqNo);

    /**
     * 根据外部借款流水号查询
     */
    FundRepayRecord findByThirdRespNo(String thirdRespNo);

    /**
     * 根据借款编号查询还款记录
     */
    List<FundRepayRecord> findByLoanNoOrderByCreateTimeDesc(String loanNo);

    /**
     * 根据客户编号查询还款记录
     */
    List<FundRepayRecord> findByCustNoOrderByCreateTimeDesc(String custNo);

    /**
     * 根据状态查询还款记录
     */
    List<FundRepayRecord> findByStatusOrderByCreateTimeDesc(String status);

    /**
     * 根据借款编号和状态查询
     */
    List<FundRepayRecord> findByLoanNoAndStatusOrderByCreateTimeDesc(String loanNo, String status);

    /**
     * 根据客户编号和时间范围查询还款记录
     */
    @Query("SELECT r FROM FundRepayRecord r WHERE r.custNo = :custNo " +
           "AND r.repaySuccessTime BETWEEN :startTime AND :endTime " +
           "ORDER BY r.repaySuccessTime DESC")
    List<FundRepayRecord> findByCustNoAndRepaySuccessTimeBetween(
        @Param("custNo") String custNo,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计客户成功还款总金额
     */
    @Query("SELECT COALESCE(SUM(r.actTotalAmt), 0) FROM FundRepayRecord r " +
           "WHERE r.custNo = :custNo AND r.status = 'SUCCESS'")
    java.math.BigDecimal sumActTotalAmtByCustNo(@Param("custNo") String custNo);

    /**
     * 统计客户还款笔数
     */
    @Query("SELECT COUNT(r) FROM FundRepayRecord r WHERE r.custNo = :custNo")
    Long countByCustNo(@Param("custNo") String custNo);

    /**
     * 统计客户成功还款笔数
     */
    @Query("SELECT COUNT(r) FROM FundRepayRecord r WHERE r.custNo = :custNo AND r.status = 'SUCCESS'")
    Long countSuccessByCustNo(@Param("custNo") String custNo);

    /**
     * 根据三方借据号查询还款记录
     */
    List<FundRepayRecord> findByThirdLoanNoOrderByCreateTimeDesc(String thirdLoanNo);
}