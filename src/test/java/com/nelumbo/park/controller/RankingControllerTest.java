package com.nelumbo.park.controller;

import com.nelumbo.park.config.security.JwtService;
import com.nelumbo.park.dto.response.TopVehicleResponse;
import com.nelumbo.park.repository.UserRepository;
import com.nelumbo.park.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RankingController.class)
class RankingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VehicleService vehicleService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(authorities = {"ADMIN", "SOCIO"})
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
    @WithMockUser(authorities = {"ADMIN", "SOCIO"})
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
