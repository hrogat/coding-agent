package com.codingagent.web.controller;

import com.codingagent.web.service.AgentClientService;
import com.codingagent.web.service.PotentiationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiController.class)
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PotentiationService potentiationService;

    @MockBean
    private AgentClientService agentClientService;

    @Test
    void testCalculatePotentiation() throws Exception {
        // Mock the service response
        when(potentiationService.calculatePotentiation(2.0, 3.0)).thenReturn(8.0);

        // Perform GET request and verify response
        mockMvc.perform(get("/api/potentiation")
                        .param("base", "2.0")
                        .param("exponent", "3.0"))
                .andExpect(status().isOk())
                .andExpect(content().string("8.0"));
    }
}