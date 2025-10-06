package com.nelumbo.park.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelumbo.park.config.TestSecurityConfig;
import com.nelumbo.park.dto.request.ParkingRequest;
import com.nelumbo.park.dto.request.ParkingUpdateRequest;
import com.nelumbo.park.dto.response.ParkingResponse;
import com.nelumbo.park.dto.response.ParkingWithVehiclesResponse;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.mapper.ParkingResponseMapper;
import com.nelumbo.park.service.ParkingService;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ParkingController.class)
@Import({ParkingControllerTest.TestConfig.class, TestSecurityConfig.class})
class ParkingControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ParkingService parkingService() {
            return Mockito.mock(ParkingService.class);
        }

        @Bean
        public ParkingResponseMapper parkingResponseMapper() {
            return Mockito.mock(ParkingResponseMapper.class);
        }
    }

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ParkingService parkingService;

    @Autowired
    private ParkingResponseMapper parkingResponseMapper;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "SOCIO"})
    void testGetParkings() throws Exception {
        ParkingResponse parkingResponse = new ParkingResponse();
        parkingResponse.setId("1");
        parkingResponse.setName("Parking Central");
        List<ParkingResponse> parkingResponses = Collections.singletonList(parkingResponse);
        when(parkingService.getAllParkings()).thenReturn(parkingResponses);

        mockMvc.perform(get("/parkings/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].name").value("Parking Central"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN", "SOCIO"})
    void testGetParkingById() throws Exception {
        String parkingId = "1";
        ParkingWithVehiclesResponse response = new ParkingWithVehiclesResponse();
        response.setId("1");
        response.setName("Parking Test");
        when(parkingService.getParkingById(parkingId)).thenReturn(response);

        mockMvc.perform(get("/parkings/{id}", parkingId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("Parking Test"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateParking() throws Exception {
        ParkingRequest parkingRequest = new ParkingRequest();
        parkingRequest.setName("Nuevo Parking Test");
        parkingRequest.setAddress("Calle Falsa 123, Springfield");
        parkingRequest.setCapacity(100);
        parkingRequest.setCostPerHour(10.5f);
        parkingRequest.setIdOwner("owner-123");

        Parking createdParking = new Parking();
        createdParking.setId("1");
        createdParking.setName("Nuevo Parking Test");

        ParkingResponse parkingResponse = new ParkingResponse();
        parkingResponse.setId("1");
        parkingResponse.setName("Nuevo Parking Test");

        when(parkingService.createParking(any(ParkingRequest.class))).thenReturn(createdParking);
        when(parkingResponseMapper.toCreateResponse(createdParking)).thenReturn(parkingResponse);

        mockMvc.perform(post("/parkings/")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parkingRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("Nuevo Parking Test"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateParking() throws Exception {
        String parkingId = "1";
        ParkingUpdateRequest parkingUpdateRequest = new ParkingUpdateRequest();
        parkingUpdateRequest.setName("Parking Actualizado");
        parkingUpdateRequest.setAddress("Calle Falsa 123, Springfield");
        parkingUpdateRequest.setCapacity(150);
        parkingUpdateRequest.setCostPerHour(12.0f);
        parkingUpdateRequest.setIdOwner("owner-123");

        Parking updatedParking = new Parking();
        updatedParking.setId("1");
        updatedParking.setName("Parking Actualizado");

        ParkingResponse parkingResponse = new ParkingResponse();
        parkingResponse.setId("1");
        parkingResponse.setName("Parking Actualizado");

        when(parkingService.updateParking(any(String.class), any(ParkingUpdateRequest.class))).thenReturn(updatedParking);
        when(parkingResponseMapper.toResponse(updatedParking)).thenReturn(parkingResponse);

        mockMvc.perform(put("/parkings/{id}", parkingId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parkingUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("Parking Actualizado"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteParking() throws Exception {
        String parkingId = "1";
        doNothing().when(parkingService).deleteParking(parkingId);

        mockMvc.perform(delete("/parkings/{id}", parkingId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Parking eliminado exitosamente"));
    }
}
