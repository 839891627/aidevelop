package com.example.aidevelop.controller;

import com.example.aidevelop.model.dto.chat.ChatConversationSummary;
import com.example.aidevelop.model.dto.chat.ChatMessageResponse;
import com.example.aidevelop.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(chatService))
            .build();
    }

    @Test
    void shouldReturnConversationSummaries() throws Exception {
        when(chatService.listConversations()).thenReturn(List.of(
            ChatConversationSummary.builder()
                .conversationId("conv-1")
                .title("请查询 USER001 的借款记录")
                .messageCount(2)
                .createdAt(LocalDateTime.of(2026, 7, 2, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 7, 2, 10, 1))
                .build()
        ));

        mockMvc.perform(get("/api/chat/conversations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].conversationId").value("conv-1"))
            .andExpect(jsonPath("$[0].title").value("请查询 USER001 的借款记录"))
            .andExpect(jsonPath("$[0].messageCount").value(2));
    }

    @Test
    void shouldReturnConversationMessages() throws Exception {
        when(chatService.getConversationMessages("conv-1")).thenReturn(List.of(
            ChatMessageResponse.builder()
                .messageId("msg-1")
                .role("USER")
                .content("请查询 USER001 的借款记录")
                .createdAt(LocalDateTime.of(2026, 7, 2, 10, 0))
                .build(),
            ChatMessageResponse.builder()
                .messageId("msg-2")
                .role("ASSISTANT")
                .content("查询到 2 条借款记录")
                .model("deepseek-v4-flash")
                .createdAt(LocalDateTime.of(2026, 7, 2, 10, 1))
                .build()
        ));

        mockMvc.perform(get("/api/chat/conv-1/messages"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].messageId").value("msg-1"))
            .andExpect(jsonPath("$[0].role").value("USER"))
            .andExpect(jsonPath("$[0].content").value("请查询 USER001 的借款记录"))
            .andExpect(jsonPath("$[1].role").value("ASSISTANT"))
            .andExpect(jsonPath("$[1].model").value("deepseek-v4-flash"));
    }
}
