package com.nelumbo.park.controller;

import com.nelumbo.park.config.TestSecurityConfig;
import com.nelumbo.park.config.security.JwtService;
import com.nelumbo.park.repository.UserRepository;
import com.nelumbo.park.dto.response.TopVehicleResponse;
import com.nelumbo.park.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RankingController.class)
@Import({RankingControllerTest.TestConfig.class, TestSecurityConfig.class})
class RankingControllerTest {

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

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN", "ROLE_SOCIO"})
    void getTopVehicles_WithAuthorizedUser_ShouldReturnTopVehicles() throws Exception {
        TopVehicleResponse topVehicle = new TopVehicleResponse("ABC-123", 10L);
        List<TopVehicleResponse> topVehicles = Collections.singletonList(topVehicle);

        when(vehicleService.getTopVehicles()).thenReturn(topVehicles);

        mockMvc.perform(get("/rankings/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].plateNumber").value("ABC-123"))
                .andExpect(jsonPath("$[0].totalVisits").value(10L));

        verify(vehicleService).getTopVehicles();
    }

    @Test
    void getTopVehicles_WithoutAuthentication_ShouldReturnForbidden() throws Exception {
        doThrow(new IllegalStateException("Service method should not be called when unauthenticated"))
                .when(vehicleService).getTopVehicles();

        mockMvc.perform(get("/rankings/"))
                .andExpect(status().isForbidden());

        verify(vehicleService, never()).getTopVehicles();
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN", "ROLE_SOCIO"})
    void getTopVehicleById_WithAuthorizedUser_ShouldReturnTopVehiclesForParking() throws Exception {
        String parkingId = "park-1";
        TopVehicleResponse topVehicle = new TopVehicleResponse("XYZ-789", 5L);
        List<TopVehicleResponse> topVehicles = Collections.singletonList(topVehicle);

        when(vehicleService.getTopVehicleById(parkingId)).thenReturn(topVehicles);

        mockMvc.perform(get("/rankings/{id}", parkingId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].plateNumber").value("XYZ-789"))
                .andExpect(jsonPath("$[0].totalVisits").value(5L));

        verify(vehicleService).getTopVehicleById(parkingId);
    }

    @Test
    void getTopVehicleById_WithoutAuthentication_ShouldReturnForbidden() throws Exception {
        String parkingId = "park-1";
        doThrow(new IllegalStateException("Service method should not be called when unauthenticated"))
                .when(vehicleService).getTopVehicleById(anyString());

        mockMvc.perform(get("/rankings/{id}", parkingId))
                .andExpect(status().isForbidden());

        verify(vehicleService, never()).getTopVehicleById(anyString());
    }
}