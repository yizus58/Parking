package com.nelumbo.park.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelumbo.park.config.TestSecurityConfig;
import com.nelumbo.park.dto.response.WeeklyParkingStatsResponse;
import com.nelumbo.park.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ParkingRankingController.class)
@Import({ParkingRankingControllerTest.TestConfig.class, TestSecurityConfig.class})
class ParkingRankingControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public VehicleService vehicleService() {
            return Mockito.mock(VehicleService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VehicleService vehicleService;

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void getParkingRanking_WithAdminRole_ShouldReturnRanking() throws Exception {
        WeeklyParkingStatsResponse statsResponse = new WeeklyParkingStatsResponse(LocalDateTime.now(), LocalDateTime.now(), Collections.emptyList());

        when(vehicleService.getParkingRanking()).thenReturn(statsResponse);

        mockMvc.perform(get("/parking-rankings/week"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.topParking").isArray());

        verify(vehicleService).getParkingRanking();
    }

    @Test
    @WithMockUser(authorities = "ROLE_SOCIO")
    void getParkingRanking_WithNonAdminRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/parking-rankings/week"))
                .andExpect(status().isForbidden());

        verify(vehicleService, never()).getParkingRanking();
    }

    @Test
    void getParkingRanking_WithoutAuthentication_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/parking-rankings/week"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(""));

        verify(vehicleService, never()).getParkingRanking();
    }
}