package com.nelumbo.park.infrastructure.adapter.in.web;

import com.nelumbo.park.application.port.in.ManageVehiclesUseCase;
import com.nelumbo.park.application.port.in.RegisterVehicleEntryUseCase;
import com.nelumbo.park.application.port.in.RegisterVehicleEntryUseCase.VehicleEntryCommand;
import com.nelumbo.park.application.port.in.RegisterVehicleExitUseCase;
import com.nelumbo.park.application.port.in.RegisterVehicleExitUseCase.VehicleExit;
import com.nelumbo.park.domain.exception.InsufficientPermissionsException;
import com.nelumbo.park.domain.exception.LimitParkingFullException;
import com.nelumbo.park.domain.exception.VehicleAlreadyInParkingException;
import com.nelumbo.park.domain.exception.VehicleNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VehicleController.class)
class VehicleControllerTest extends WebLayerTestSupport {

    private static final String CREATE = "{\"plateNumber\":\"ABC123\",\"modelVehicle\":\"Mazda\",\"idParking\":\"park-1\"}";
    private static final String EXIT = "{\"plateNumber\":\"abc123\",\"modelVehicle\":\"Mazda\",\"idParking\":\"park-1\"}";

    @MockitoBean
    private ManageVehiclesUseCase manageVehicles;
    @MockitoBean
    private RegisterVehicleEntryUseCase registerEntry;
    @MockitoBean
    private RegisterVehicleExitUseCase registerExit;

    @Test
    @WithMockUser(authorities = "ADMIN")
    void list_includesParkingWithOwnerAndAdmin() throws Exception {
        when(manageVehicles.listVehicles()).thenReturn(List.of(WebFixtures.inside()));

        mockMvc.perform(get("/vehicles/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("veh-1"))
                .andExpect(jsonPath("$[0].plateNumber").value("ABC123"))
                .andExpect(jsonPath("$[0].model").value("Mazda"))
                .andExpect(jsonPath("$[0].entryTime").value(WebFixtures.ENTRY.toEpochMilli()))
                .andExpect(jsonPath("$[0].exitTime").isEmpty())
                .andExpect(jsonPath("$[0].costPerHour").value(2500.0))
                .andExpect(jsonPath("$[0].status").value("IN"))
                .andExpect(jsonPath("$[0].parking.id").value("park-1"))
                .andExpect(jsonPath("$[0].parking.owner.id").value("owner-1"))
                .andExpect(jsonPath("$[0].admin.id").value("owner-1"))
                .andExpect(jsonPath("$[0].admin.email").value("socio@example.com"));
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void get_asPartner() throws Exception {
        when(manageVehicles.getVehicle("veh-1")).thenReturn(WebFixtures.inside());

        mockMvc.perform(get("/vehicles/veh-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("veh-1"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void get_asAdmin_forbiddenAsBefore() throws Exception {
        mockMvc.perform(get("/vehicles/veh-1")).andExpect(status().isForbidden());
        verify(manageVehicles, never()).getVehicle(any());
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void get_otherPartnersVehicle_forbidden() throws Exception {
        when(manageVehicles.getVehicle("veh-1")).thenThrow(new InsufficientPermissionsException());
        mockMvc.perform(get("/vehicles/veh-1")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void create_returnsEntryWithBogotaFormattedDate() throws Exception {
        when(registerEntry.registerEntry(new VehicleEntryCommand("ABC123", "Mazda", "park-1")))
                .thenReturn(WebFixtures.inside());

        mockMvc.perform(post("/vehicles/").contentType(MediaType.APPLICATION_JSON).content(CREATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plateNumber").value("ABC123"))
                .andExpect(jsonPath("$.model").value("Mazda"))
                .andExpect(jsonPath("$.entryTime").value("20-11-2026 09:15"))
                .andExpect(jsonPath("$.exitTime").isEmpty())
                .andExpect(jsonPath("$.costPerHour").value(2500.0))
                .andExpect(jsonPath("$.status").value("IN"))
                .andExpect(jsonPath("$.id").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void create_invalid_returns400() throws Exception {
        mockMvc.perform(post("/vehicles/").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plateNumber\":\"\",\"modelVehicle\":\"\",\"idParking\":\"\"}"))
                .andExpect(status().isBadRequest());
        verify(registerEntry, never()).registerEntry(any());
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void create_parkingFull_returns409WithMessage() throws Exception {
        when(registerEntry.registerEntry(any()))
                .thenThrow(new LimitParkingFullException("El limite de vehiculos en parking X ya ha sido alcanzado"));

        mockMvc.perform(post("/vehicles/").contentType(MediaType.APPLICATION_JSON).content(CREATE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El limite de vehiculos en parking X ya ha sido alcanzado"));
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void create_alreadyInside_returns409() throws Exception {
        when(registerEntry.registerEntry(any())).thenThrow(new VehicleAlreadyInParkingException("dentro"));

        mockMvc.perform(post("/vehicles/").contentType(MediaType.APPLICATION_JSON).content(CREATE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("dentro"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void create_asAdmin_forbidden() throws Exception {
        mockMvc.perform(post("/vehicles/").contentType(MediaType.APPLICATION_JSON).content(CREATE))
                .andExpect(status().isForbidden());
        verify(registerEntry, never()).registerEntry(any());
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void exit_returnsFormattedPesos() throws Exception {
        when(registerExit.registerExit("abc123")).thenReturn(new VehicleExit(WebFixtures.out(), 7500f));

        String body = mockMvc.perform(put("/vehicles/").contentType(MediaType.APPLICATION_JSON).content(EXIT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plateNumber").value("ABC123"))
                .andExpect(jsonPath("$.entryTime").value("20-11-2026 09:15"))
                .andExpect(jsonPath("$.exitTime").value("20-11-2026 11:20"))
                .andExpect(jsonPath("$.status").value("OUT"))
                .andReturn().getResponse().getContentAsString();
        String totalCost = objectMapper.readTree(body).get("totalCost").asText();
        org.junit.jupiter.api.Assertions.assertTrue(totalCost.startsWith("$") && totalCost.endsWith("7.500"), totalCost);
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void exit_notInside_returns404() throws Exception {
        when(registerExit.registerExit(any())).thenThrow(new VehicleNotFoundException());

        mockMvc.perform(put("/vehicles/").contentType(MediaType.APPLICATION_JSON).content(EXIT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("El vehículo no existe"));
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void delete_returnsMessage() throws Exception {
        mockMvc.perform(delete("/vehicles/veh-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Vehículo eliminado exitosamente"));
        verify(manageVehicles).deleteVehicle("veh-1");
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void delete_vehicleOfAnotherPartnersParking_returns403() throws Exception {
        org.mockito.Mockito.doThrow(new com.nelumbo.park.domain.exception.InsufficientPermissionsException())
                .when(manageVehicles).deleteVehicle("veh-2");

        mockMvc.perform(delete("/vehicles/veh-2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No tienes permisos para realizar esta acción"));
    }

    @Test
    void anonymous_unauthorized() throws Exception {
        mockMvc.perform(post("/vehicles/").contentType(MediaType.APPLICATION_JSON).content(CREATE))
                .andExpect(status().isUnauthorized());
        verify(registerEntry, never()).registerEntry(any());
    }
}
