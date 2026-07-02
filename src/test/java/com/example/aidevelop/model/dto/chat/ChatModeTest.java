package com.example.aidevelop.model.dto.chat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatModeTest {

    @Test
    void defaultsBlankModeToGeneralChat() {
        assertThat(ChatMode.from(null)).isEqualTo(ChatMode.GENERAL);
        assertThat(ChatMode.from(" ")).isEqualTo(ChatMode.GENERAL);
    }

    @Test
    void resolvesSupportedExplicitModes() {
        assertThat(ChatMode.from("general")).isEqualTo(ChatMode.GENERAL);
        assertThat(ChatMode.from("financial_rag")).isEqualTo(ChatMode.FINANCIAL_RAG);
        assertThat(ChatMode.from("auto")).isEqualTo(ChatMode.AUTO);
    }

    @Test
    void fallsBackToGeneralForUnknownMode() {
        assertThat(ChatMode.from("legacy-normal")).isEqualTo(ChatMode.GENERAL);
    }
}
