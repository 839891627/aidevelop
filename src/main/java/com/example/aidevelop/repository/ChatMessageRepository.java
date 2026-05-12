package com.example.aidevelop.repository;

import com.example.aidevelop.model.entity.ChatMessageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    List<ChatMessageEntity> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    boolean existsByMessageId(String messageId);

    void deleteByConversationId(String conversationId);

    @Query("select distinct c.conversationId from ChatMessageEntity c")
    List<String> findDistinctConversationIds();
}
