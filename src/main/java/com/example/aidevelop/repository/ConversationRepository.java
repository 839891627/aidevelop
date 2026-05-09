package com.example.aidevelop.repository;

import com.example.aidevelop.model.entity.Conversation;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话存储 - 内存实现
 * 使用线程安全的 ConcurrentHashMap
 *
 * 边界说明：
 * 1) 当前仅用于教学与本地开发，数据不会落库；
 * 2) 应用重启后会丢失所有会话历史；
 * 3) 若用于生产，请替换为 Redis / MySQL 等持久化实现。
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
