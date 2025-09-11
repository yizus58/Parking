package com.nelumbo.park.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nelumbo.park.config.TestSecurityConfig;
import com.nelumbo.park.config.security.JwtService;
import com.nelumbo.park.dto.request.VehicleCreateRequest;
import com.nelumbo.park.dto.response.VehicleCreateResponse;
import com.nelumbo.park.dto.response.VehicleExitResponse;
import com.nelumbo.park.dto.response.VehicleResponse;
import com.nelumbo.park.dto.request.VehicleUpdateRequest;
import com.nelumbo.park.entity.Vehicle;
import com.nelumbo.park.enums.VehicleStatus;
import com.nelumbo.park.mapper.VehicleResponseMapper;
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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VehicleController.class)
@Import(TestSecurityConfig.class)
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VehicleService vehicleService;

    @MockBean
    private VehicleResponseMapper vehicleResponseMapper;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(authorities = {"ADMIN", "SOCIO"})
    void getVehicles_WithAuthorizedUser_ShouldReturnVehicles() throws Exception {
        Vehicle vehicle = new Vehicle();
        vehicle.setId("veh1");
        vehicle.setPlateNumber("ABC-123");
        List<Vehicle> vehicles = Collections.singletonList(vehicle);

        VehicleResponse vehicleResponse = new VehicleResponse();
        vehicleResponse.setId("veh1");
        vehicleResponse.setPlateNumber("ABC-123");
        List<VehicleResponse> vehicleResponses = Collections.singletonList(vehicleResponse);

        when(vehicleService.getAllVehicles()).thenReturn(vehicles);
        when(vehicleResponseMapper.toResponseList(vehicles)).thenReturn(vehicleResponses);

        mockMvc.perform(get("/vehicles/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value("veh1"))
                .andExpect(jsonPath("$[0].plateNumber").value("ABC-123"));

        verify(vehicleService).getAllVehicles();
        verify(vehicleResponseMapper).toResponseList(vehicles);
    }

    @Test
    void getVehicles_WithoutAuthentication_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/vehicles/"))
                .andExpect(status().isForbidden());

        verify(vehicleService, never()).getAllVehicles();
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void getVehicleById_WithSocioRole_ShouldReturnVehicle() throws Exception {
        String vehicleId = "veh1";
        Vehicle vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        vehicle.setPlateNumber("ABC-123");

        VehicleResponse vehicleResponse = new VehicleResponse();
        vehicleResponse.setId(vehicleId);
        vehicleResponse.setPlateNumber("ABC-123");

        when(vehicleService.getVehicleById(vehicleId)).thenReturn(vehicle);
        when(vehicleResponseMapper.toResponse(vehicle)).thenReturn(vehicleResponse);

        mockMvc.perform(get("/vehicles/{id}", vehicleId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(vehicleId))
                .andExpect(jsonPath("$.plateNumber").value("ABC-123"));

        verify(vehicleService).getVehicleById(vehicleId);
        verify(vehicleResponseMapper).toResponse(vehicle);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void getVehicleById_WithAdminRole_ShouldReturnForbidden() throws Exception {
        String vehicleId = "veh1";
        mockMvc.perform(get("/vehicles/{id}", vehicleId))
                .andExpect(status().isForbidden());

        verify(vehicleService, never()).getVehicleById(anyString());
    }

    @Test
    void getVehicleById_WithoutAuthentication_ShouldReturnForbidden() throws Exception {
        String vehicleId = "veh1";
        mockMvc.perform(get("/vehicles/{id}", vehicleId))
                .andExpect(status().isForbidden());

        verifyNoInteractions(vehicleService);
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void createVehicle_WithSocioRoleAndValidData_ShouldReturnCreatedVehicle() throws Exception {
        VehicleCreateRequest createRequest = new VehicleCreateRequest(
                "DEF-456", "Model X", new Date(), null, "park1", "admin1", 15.0f, VehicleStatus.IN
        );
        VehicleCreateResponse createResponse = new VehicleCreateResponse("DEF-456", "Model X", new Date(), null, 15.0f, VehicleStatus.IN);

        when(vehicleService.createVehicle(any(VehicleCreateRequest.class))).thenReturn(createResponse);

        mockMvc.perform(post("/vehicles/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.plateNumber").value("DEF-456"))
                .andExpect(jsonPath("$.model").value("Model X"));

        verify(vehicleService).createVehicle(any(VehicleCreateRequest.class));
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void createVehicle_WithSocioRoleAndInvalidData_ShouldReturnBadRequest() throws Exception {
        VehicleCreateRequest createRequest = new VehicleCreateRequest(
                "", "", null, null, "", "", null, null
        );

        mockMvc.perform(post("/vehicles/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());

        verify(vehicleService, never()).createVehicle(any(VehicleCreateRequest.class));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createVehicle_WithAdminRole_ShouldReturnForbidden() throws Exception {
        VehicleCreateRequest createRequest = new VehicleCreateRequest(
                "DEF-456", "Model X", new Date(), null, "park1", "admin1", 15.0f, VehicleStatus.IN
        );
        mockMvc.perform(post("/vehicles/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());

        verify(vehicleService, never()).createVehicle(any(VehicleCreateRequest.class));
    }

    @Test
    void createVehicle_WithoutAuthentication_ShouldReturnForbidden() throws Exception {
        VehicleCreateRequest createRequest = new VehicleCreateRequest(
                "DEF-456", "Model X", new Date(), null, "park1", "admin1", 15.0f, VehicleStatus.IN
        );
        mockMvc.perform(post("/vehicles/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(vehicleService);
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void exitVehicle_WithSocioRoleAndValidData_ShouldReturnExitResponse() throws Exception {
        VehicleUpdateRequest updateRequest = new VehicleUpdateRequest(
                "ABC-123", "Model Y", new Date(), new Date(), "park1", "admin1", 10.0f, VehicleStatus.OUT
        );
        VehicleExitResponse exitResponse = new VehicleExitResponse("ABC-123", "Model Y", new Date(), new Date(), 10.0f, VehicleStatus.OUT, 25.0f);

        when(vehicleService.exitVehicle(any(VehicleUpdateRequest.class))).thenReturn(exitResponse);

        mockMvc.perform(put("/vehicles/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.plateNumber").value("ABC-123"))
                .andExpect(jsonPath("$.model").value("Model Y"));

        verify(vehicleService).exitVehicle(any(VehicleUpdateRequest.class));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void exitVehicle_WithAdminRole_ShouldReturnForbidden() throws Exception {
        VehicleUpdateRequest updateRequest = new VehicleUpdateRequest(
                "ABC-123", "Model Y", new Date(), new Date(), "park1", "admin1", 10.0f, VehicleStatus.OUT
        );
        mockMvc.perform(put("/vehicles/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        verify(vehicleService, never()).exitVehicle(any(VehicleUpdateRequest.class));
    }

    @Test
    void exitVehicle_WithoutAuthentication_ShouldReturnForbidden() throws Exception {
        VehicleUpdateRequest updateRequest = new VehicleUpdateRequest(
                "ABC-123", "Model Y", new Date(), new Date(), "park1", "admin1", 10.0f, VehicleStatus.OUT
        );
        mockMvc.perform(put("/vehicles/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(vehicleService);
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void deleteVehicle_WithSocioRole_ShouldReturnOk() throws Exception {
        String vehicleId = "veh1";
        doNothing().when(vehicleService).deleteVehicle(vehicleId);

        mockMvc.perform(delete("/vehicles/{id}", vehicleId))
                .andExpect(status().isOk())
                .andExpect(content().string("Vehículo eliminado exitosamente"));

        verify(vehicleService).deleteVehicle(vehicleId);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void deleteVehicle_WithAdminRole_ShouldReturnForbidden() throws Exception {
        String vehicleId = "veh1";
        mockMvc.perform(delete("/vehicles/{id}", vehicleId))
                .andExpect(status().isForbidden());

        verify(vehicleService, never()).deleteVehicle(anyString());
    }

    @Test
    void deleteVehicle_WithoutAuthentication_ShouldReturnForbidden() throws Exception {
        String vehicleId = "veh1";
        mockMvc.perform(delete("/vehicles/{id}", vehicleId))
                .andExpect(status().isForbidden());

        verifyNoInteractions(vehicleService);
    }
}
