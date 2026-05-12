package com.example.aidevelop.repository;

import com.example.aidevelop.model.entity.PromptTemplateEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplateEntity, Long> {

    Optional<PromptTemplateEntity> findFirstByPromptKeyAndEnvAndStatusOrderByVersionDesc(
            String promptKey, String env, String status);

    Optional<PromptTemplateEntity> findFirstByPromptKeyAndEnvOrderByVersionDesc(
            String promptKey, String env);

    Optional<PromptTemplateEntity> findByPromptKeyAndEnvAndVersion(
            String promptKey, String env, Integer version);

    List<PromptTemplateEntity> findByPromptKeyAndEnvOrderByVersionDesc(String promptKey, String env);
}
