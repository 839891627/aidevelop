package com.example.aidevelop.repository;

import com.example.aidevelop.model.entity.FundLoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 借款表Repository
 */
@Repository
public interface FundLoanRepository extends JpaRepository<FundLoan, Long> {

    /**
     * 根据业务流水号查询
     */
    FundLoan findByBizSerial(String bizSerial);

    /**
     * 根据三方借据号查询
     */
    FundLoan findByThirdLoanNo(String thirdLoanNo);

    /**
     * 根据客户编号查询借款列表
     */
    List<FundLoan> findByCustNoOrderByCreateTimeDesc(String custNo);

    /**
     * 根据合同号查询
     */
    FundLoan findByContractNumber(String contractNumber);

    /**
     * 根据状态查询
     */
    List<FundLoan> findByStatusOrderByCreateTimeDesc(String status);

    /**
     * 查询客户在指定时间范围的借款
     */
    @Query("SELECT f FROM FundLoan f WHERE f.custNo = :custNo " +
           "AND f.loanSuccessTime BETWEEN :startTime AND :endTime " +
           "ORDER BY f.loanSuccessTime DESC")
    List<FundLoan> findByCustNoAndLoanSuccessTimeBetween(
        @Param("custNo") String custNo,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计客户借款数量
     */
    @Query("SELECT COUNT(f) FROM FundLoan f WHERE f.custNo = :custNo")
    Long countByCustNo(@Param("custNo") String custNo);

    /**
     * 统计客户借款总金额
     */
    @Query("SELECT COALESCE(SUM(f.loanAmt), 0) FROM FundLoan f " +
           "WHERE f.custNo = :custNo AND f.status = 'SUCCESS'")
    java.math.BigDecimal sumLoanAmtByCustNo(@Param("custNo") String custNo);
}
