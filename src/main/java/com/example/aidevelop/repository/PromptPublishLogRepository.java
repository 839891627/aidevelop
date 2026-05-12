package com.example.aidevelop.repository;

import com.example.aidevelop.model.entity.PromptPublishLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromptPublishLogRepository extends JpaRepository<PromptPublishLogEntity, Long> {
}
