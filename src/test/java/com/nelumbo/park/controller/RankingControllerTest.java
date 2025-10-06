package com.nelumbo.park.controller;

import com.nelumbo.park.dto.response.TopVehicleResponse;
import com.nelumbo.park.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RankingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VehicleService vehicleService;

    @InjectMocks
    private RankingController rankingController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(rankingController).build();
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
        mockMvc.perform(get("/rankings/{id}", parkingId))
                .andExpect(status().isForbidden());

        verify(vehicleService, never()).getTopVehicleById(anyString());
    }
}