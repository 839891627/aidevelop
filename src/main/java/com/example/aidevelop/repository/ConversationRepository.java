package com.example.aidevelop.repository;

import com.example.aidevelop.model.entity.Conversation;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话存储 - 内存实现
 * 使用线程安全的 ConcurrentHashMap
 * 后续可以替换为 Redis、MySQL 等持久化存储
 */
@Repository
public class ConversationRepository {

    // 使用线程安全的 Map 作为内存存储
    private final Map<String, Conversation> storage = new ConcurrentHashMap<>();

    public Conversation save(Conversation conversation) {
        storage.put(conversation.getConversationId(), conversation);
        return conversation;
    }

    public Optional<Conversation> findById(String conversationId) {
        return Optional.ofNullable(storage.get(conversationId));
    }

    public List<Conversation> findAll() {
        return new ArrayList<>(storage.values());
    }

    public void delete(String conversationId) {
        storage.remove(conversationId);
    }

    public void deleteAll() {
        storage.clear();
    }
}
