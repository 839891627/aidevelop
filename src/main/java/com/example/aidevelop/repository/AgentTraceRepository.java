package com.example.aidevelop.repository;

import com.example.aidevelop.model.entity.AgentTraceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentTraceRepository extends JpaRepository<AgentTraceEntity, Long> {

    Optional<AgentTraceEntity> findByTraceId(String traceId);
}
