package com.example.aidevelop.repository;

import com.example.aidevelop.model.entity.RepaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 还款记录表数据访问接口（演示版）
 */
@Repository
public interface RepaymentRecordRepository extends JpaRepository<RepaymentRecord, Long> {

    /**
     * 根据用户编号查询还款记录
     */
    List<RepaymentRecord> findByUserNo(String userNo);

    /**
     * 根据业务流水号查询
     */
    Optional<RepaymentRecord> findByBizSerial(String bizSerial);

    /**
     * 根据借款编号查询还款记录
     */
    List<RepaymentRecord> findByLoanNo(String loanNo);

    /**
     * 根据状态查询还款记录
     */
    List<RepaymentRecord> findByStatus(String status);

    /**
     * 查询用户的最新还款记录
     */
    @Query("SELECT r FROM RepaymentRecord r WHERE r.userNo = :userNo ORDER BY r.createTime DESC")
    List<RepaymentRecord> findLatestByUserNo(@Param("userNo") String userNo);

    /**
     * 查询待处理的还款记录
     */
    List<RepaymentRecord> findByStatusIn(List<String> statuses);
}
