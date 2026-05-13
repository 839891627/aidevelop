package com.example.aidevelop.agent.controller;

import com.example.aidevelop.agent.model.AgentRequest;
import com.example.aidevelop.agent.model.AgentResponse;
import com.example.aidevelop.agent.model.AgentStep;
import com.example.aidevelop.agent.service.AgentService;
import com.example.aidevelop.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AgentControllerIntegrationTest {

    @Mock
    private AgentService agentService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AgentController(agentService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void shouldReturnAgentResponse() throws Exception {
        AgentResponse mockResponse = AgentResponse.builder()
            .traceId("trace-1")
            .routeType("HYBRID")
            .finalAnswer("这是 Agent 回答")
            .completed(true)
            .executedSteps(3)
            .responseTimeMs(120)
            .steps(List.of(AgentStep.builder().stepIndex(1).success(true).build()))
            .build();
        when(agentService.chat(any())).thenReturn(mockResponse);

        AgentRequest request = new AgentRequest();
        request.setMessage("请先查借款，再回答");

        mockMvc.perform(post("/api/agent/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.traceId").value("trace-1"))
            .andExpect(jsonPath("$.routeType").value("HYBRID"))
            .andExpect(jsonPath("$.finalAnswer").value("这是 Agent 回答"));
    }
}
