package com.nelumbo.park.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelumbo.park.config.TestSecurityConfig;
import com.nelumbo.park.config.security.JwtAuthenticationFilter;
import com.nelumbo.park.config.security.JwtService;
import com.nelumbo.park.dto.response.WeeklyPartnerStatsResponse;
import com.nelumbo.park.repository.UserRepository;
import com.nelumbo.park.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = PartnersRankingController.class,
        excludeAutoConfiguration = {
                UserDetailsServiceAutoConfiguration.class,
                SecurityAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@Import({PartnersRankingControllerTest.TestConfig.class, TestSecurityConfig.class})
class PartnersRankingControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public VehicleService vehicleService() {
            return Mockito.mock(VehicleService.class);
        }
    }

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private VehicleService vehicleService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

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
                .andExpect(status().isForbidden());

        verify(vehicleService, never()).getPartnersRanking();
    }
}