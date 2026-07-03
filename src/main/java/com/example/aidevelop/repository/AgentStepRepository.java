package com.example.aidevelop.repository;

import com.example.aidevelop.model.entity.AgentStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentStepRepository extends JpaRepository<AgentStepEntity, Long> {

    List<AgentStepEntity> findByTraceIdOrderByStepIndexAsc(String traceId);
}
