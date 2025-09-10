package com.nelumbo.park.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelumbo.park.config.TestSecurityConfig;
import com.nelumbo.park.config.security.JwtService;
import com.nelumbo.park.dto.response.IndicatorResponse;
import com.nelumbo.park.repository.UserRepository;
import com.nelumbo.park.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(IndicatorController.class)
@Import(TestSecurityConfig.class)
class IndicatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VehicleService vehicleService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private List<IndicatorResponse> indicatorResponseList;

    @BeforeEach
    void setUp() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("America/Bogota"));
        Date entryDate = sdf.parse("04-09-2025 10:47");

        IndicatorResponse.ParkingSimpleResponse parking = new IndicatorResponse.ParkingSimpleResponse("parqueadero El bicho");
        IndicatorResponse indicatorResponse = new IndicatorResponse("POS-555", "Mitsubishi Carisma", entryDate, null, parking);
        indicatorResponseList = Collections.singletonList(indicatorResponse);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void getFirstTimeParkedVehicles_WithAdminRole_ShouldReturnVehiclesList() throws Exception {

        when(vehicleService.getFirstTimeParkedVehicles()).thenReturn(indicatorResponseList);

        mockMvc.perform(get("/indicators/")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].plateNumber").value("POS-555"))
                .andExpect(jsonPath("$[0].modelVehicle").value("Mitsubishi Carisma"))
                .andExpect(jsonPath("$[0].entryTime").value("04-09-2025 10:47"))
                .andExpect(jsonPath("$[0].parking.name").value("parqueadero El bicho"));

        verify(vehicleService, times(1)).getFirstTimeParkedVehicles();
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void getFirstTimeParkedVehicles_WithSocioRole_ShouldReturnVehiclesList() throws Exception {

        when(vehicleService.getFirstTimeParkedVehicles()).thenReturn(indicatorResponseList);

        mockMvc.perform(get("/indicators/")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1));

        verify(vehicleService, times(1)).getFirstTimeParkedVehicles();
    }

    @Test
    void getFirstTimeParkedVehicles_WithoutAuthentication_ShouldReturnForbidden() throws Exception {

        mockMvc.perform(get("/indicators/")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(vehicleService, never()).getFirstTimeParkedVehicles();
    }

    @Test
    @WithMockUser(authorities = {})
    void getFirstTimeParkedVehicles_WithNoAuthorities_ShouldReturnForbidden() throws Exception {

        mockMvc.perform(get("/indicators/")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(vehicleService, never()).getFirstTimeParkedVehicles();
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void getFirstTimeParkedVehicles_WhenNoVehicles_ShouldReturnEmptyList() throws Exception {

        when(vehicleService.getFirstTimeParkedVehicles()).thenReturn(Collections.emptyList());


        mockMvc.perform(get("/indicators/")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));

        verify(vehicleService, times(1)).getFirstTimeParkedVehicles();
    }
}
