package com.example.aidevelop.repository;

import com.example.aidevelop.model.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 借款表数据访问接口（演示版）
 */
@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    /**
     * 根据用户编号查询借款记录
     */
    List<Loan> findByUserNo(String userNo);

    /**
     * 根据业务流水号查询
     */
    Optional<Loan> findByBizSerial(String bizSerial);

    /**
     * 根据状态查询借款记录
     */
    List<Loan> findByStatus(String status);

    /**
     * 查询用户的最新借款记录
     */
    @Query("SELECT l FROM Loan l WHERE l.userNo = :userNo ORDER BY l.createTime DESC")
    List<Loan> findLatestByUserNo(@Param("userNo") String userNo);

    /**
     * 查询可能逾期的借款（成功放款且超过一定时间）
     */
    @Query("SELECT l FROM Loan l WHERE l.status = 'SUCCESS' AND l.loanSuccessTime < :beforeTime")
    List<Loan> findPotentialOverdueLoans(@Param("beforeTime") LocalDateTime beforeTime);
}
