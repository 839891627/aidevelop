package com.example.aidevelop.repository;

import com.example.aidevelop.model.entity.ChatMessageEntity;
import com.example.aidevelop.model.entity.Conversation;
import com.example.aidevelop.model.entity.Message;
import com.example.aidevelop.model.entity.MessageRole;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * 对话存储 - MySQL 持久化实现。
 */
@Repository
public class ConversationRepository {

    private final ChatMessageRepository chatMessageRepository;

    public ConversationRepository(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public Conversation save(Conversation conversation) {
        for (Message message : conversation.getMessages()) {
            if (message.getId() == null || message.getId().isBlank()) {
                message.setId(UUID.randomUUID().toString());
            }
            if (!chatMessageRepository.existsByMessageId(message.getId())) {
                chatMessageRepository.save(toEntity(conversation.getConversationId(), message));
            }
        }
        return conversation;
    }

    public Optional<Conversation> findById(String conversationId) {
        List<ChatMessageEntity> entities =
                chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        if (entities.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toConversation(conversationId, entities));
    }

    public List<Conversation> findAll() {
        List<String> conversationIds = chatMessageRepository.findDistinctConversationIds();
        List<Conversation> result = new ArrayList<>(conversationIds.size());
        for (String conversationId : conversationIds) {
            findById(conversationId).ifPresent(result::add);
        }
        result.sort(Comparator.comparing(Conversation::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    @Transactional
    public void delete(String conversationId) {
        chatMessageRepository.deleteByConversationId(conversationId);
    }

    @Transactional
    public void deleteAll() {
        chatMessageRepository.deleteAllInBatch();
    }

    private ChatMessageEntity toEntity(String conversationId, Message message) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setMessageId(message.getId());
        entity.setConversationId(conversationId);
        entity.setRole(message.getRole().name());
        entity.setContent(message.getContent());
        entity.setModel(message.getModel());
        entity.setCreatedAt(message.getTimestamp() != null ? message.getTimestamp() : LocalDateTime.now());
        return entity;
    }

    private Conversation toConversation(String conversationId, List<ChatMessageEntity> entities) {
        Conversation conversation = new Conversation();
        conversation.setConversationId(conversationId);

        List<Message> messages = entities.stream()
                .map(this::toMessage)
                .toList();
        conversation.setMessages(new ArrayList<>(messages));

        LocalDateTime createdAt = entities.get(0).getCreatedAt();
        LocalDateTime updatedAt = entities.get(entities.size() - 1).getCreatedAt();
        conversation.setCreatedAt(createdAt);
        conversation.setUpdatedAt(updatedAt);
        return conversation;
    }

    private Message toMessage(ChatMessageEntity entity) {
        Message message = new Message();
        message.setId(entity.getMessageId());
        message.setRole(MessageRole.valueOf(entity.getRole()));
        message.setContent(entity.getContent());
        message.setTimestamp(entity.getCreatedAt());
        message.setModel(entity.getModel());
        return message;
    }
}
