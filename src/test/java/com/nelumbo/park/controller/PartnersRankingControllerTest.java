package com.nelumbo.park.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelumbo.park.config.TestSecurityConfig;
import com.nelumbo.park.config.security.JwtService;
import com.nelumbo.park.dto.response.WeeklyPartnerStatsResponse;
import com.nelumbo.park.repository.UserRepository;
import com.nelumbo.park.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PartnersRankingController.class)
@Import(TestSecurityConfig.class)
class PartnersRankingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VehicleService vehicleService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void getPartnersRanking_WithAdminRole_ShouldReturnRanking() throws Exception {
        WeeklyPartnerStatsResponse statsResponse = new WeeklyPartnerStatsResponse(LocalDateTime.now(), LocalDateTime.now(), Collections.emptyList());

        when(vehicleService.getPartnersRanking()).thenReturn(statsResponse);

        mockMvc.perform(get("/partners-rankings/week"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.topPartners").isArray());

        verify(vehicleService).getPartnersRanking();
    }

    @Test
    @WithMockUser(authorities = "ROLE_SOCIO")
    void getPartnersRanking_WithNonAdminRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/partners-rankings/week"))
                .andExpect(status().isForbidden());

        verify(vehicleService, never()).getPartnersRanking();
    }

    @Test
    void getPartnersRanking_WithoutAuthentication_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/partners-rankings/week"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(""));

        verify(vehicleService, never()).getPartnersRanking();
    }
}