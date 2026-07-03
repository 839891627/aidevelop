package com.example.aidevelop.model.dto.chat;

import java.util.Locale;

public enum ChatMode {
    // 常规聊天：不挂载金融 RAG，也不暴露业务工具，避免普通问题被金融提示词影响。
    GENERAL("general"),
    // 金融 RAG：只做金融助贷知识库问答，通过 RagFacade 检索证据。
    FINANCIAL_RAG("financial_rag"),
    // 自动路由：保留旧链路，由 IntentRoutingService 判断 Tool/RAG/Hybrid。
    AUTO("auto");

    private final String value;

    ChatMode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static ChatMode from(String value) {
        if (value == null || value.isBlank()) {
            // 前端未传 mode 时默认常规聊天，保证“普通聊天”不会意外进入金融链路。
            return GENERAL;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ChatMode mode : values()) {
            if (mode.value.equals(normalized)) {
                return mode;
            }
        }
        // 未知 mode 按常规聊天处理，避免错误输入触发更高权限的工具或 RAG 能力。
        return GENERAL;
    }
}
