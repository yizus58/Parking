package com.nelumbo.park.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelumbo.park.config.TestSecurityConfig;
import com.nelumbo.park.config.security.JwtService;
import com.nelumbo.park.dto.request.ParkingRequest;
import com.nelumbo.park.dto.request.ParkingUpdateRequest;
import com.nelumbo.park.dto.response.ParkingResponse;
import com.nelumbo.park.dto.response.ParkingWithVehiclesResponse;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.mapper.ParkingResponseMapper;
import com.nelumbo.park.repository.UserRepository;
import com.nelumbo.park.service.ParkingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ParkingController.class)
@Import(TestSecurityConfig.class)
class ParkingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ParkingService parkingService;

    @MockBean
    private ParkingResponseMapper parkingResponseMapper;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(authorities = {"ADMIN", "SOCIO"})
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
    @WithMockUser(authorities = {"ADMIN", "SOCIO"})
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
    @WithMockUser(authorities = "ADMIN")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parkingRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("Nuevo Parking Test"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parkingUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("Parking Actualizado"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void testDeleteParking() throws Exception {
        String parkingId = "1";
        doNothing().when(parkingService).deleteParking(parkingId);

        mockMvc.perform(delete("/parkings/{id}", parkingId))
                .andExpect(status().isOk())
                .andExpect(content().string("Parking eliminado exitosamente"));
    }
}
