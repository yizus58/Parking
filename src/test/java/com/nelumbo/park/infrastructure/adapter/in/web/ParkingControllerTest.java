package com.nelumbo.park.infrastructure.adapter.in.web;

import com.nelumbo.park.application.port.in.ManageParkingsUseCase;
import com.nelumbo.park.application.port.in.ManageParkingsUseCase.ParkingCommand;
import com.nelumbo.park.application.port.in.ManageParkingsUseCase.ParkingDetails;
import com.nelumbo.park.application.port.in.ManageParkingsUseCase.VisibleParkings;
import com.nelumbo.park.domain.exception.NoAssociatedParkingException;
import com.nelumbo.park.domain.exception.ParkingNotFoundException;
import com.nelumbo.park.domain.exception.UserNotFoundException;
import com.nelumbo.park.domain.model.Parking;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ParkingController.class)
class ParkingControllerTest extends WebLayerTestSupport {

    private static final String BODY = "{\"name\":\"Parking Centro\",\"address\":\"Calle 10 # 20-30\","
            + "\"capacity\":20,\"costPerHour\":2500,\"idOwner\":\"owner-1\"}";
    private static final ParkingCommand COMMAND =
            new ParkingCommand("Parking Centro", "Calle 10 # 20-30", 20, 2500f, "owner-1");

    @MockitoBean
    private ManageParkingsUseCase manageParkings;

    @Test
    @WithMockUser(authorities = "ADMIN")
    void list_asAdmin_includesOwner() throws Exception {
        when(manageParkings.listParkings()).thenReturn(new VisibleParkings(List.of(WebFixtures.parking()), true));

        mockMvc.perform(get("/parkings/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("park-1"))
                .andExpect(jsonPath("$[0].name").value("Parking Centro"))
                .andExpect(jsonPath("$[0].capacity").value(20))
                .andExpect(jsonPath("$[0].costPerHour").value(2500.0))
                .andExpect(jsonPath("$[0].owner.id").value("owner-1"))
                .andExpect(jsonPath("$[0].owner.username").value("socio"));
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void list_asPartner_omitsOwner() throws Exception {
        when(manageParkings.listParkings()).thenReturn(new VisibleParkings(List.of(WebFixtures.parking()), false));

        mockMvc.perform(get("/parkings/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("park-1"))
                .andExpect(jsonPath("$[0].owner").doesNotExist());
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void list_partnerWithoutParkings_returns401AsBefore() throws Exception {
        when(manageParkings.listParkings()).thenThrow(new NoAssociatedParkingException());

        mockMvc.perform(get("/parkings/"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("No tienes parkings asociados"));
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void get_includesVehicles() throws Exception {
        when(manageParkings.getParking("park-1"))
                .thenReturn(new ParkingDetails(WebFixtures.parking(), List.of(WebFixtures.inside())));

        mockMvc.perform(get("/parkings/park-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("park-1"))
                .andExpect(jsonPath("$.owner").doesNotExist())
                .andExpect(jsonPath("$.vehicles[0].id").value("veh-1"))
                .andExpect(jsonPath("$.vehicles[0].plateNumber").value("ABC123"))
                .andExpect(jsonPath("$.vehicles[0].model").value("Mazda"))
                .andExpect(jsonPath("$.vehicles[0].status").value("IN"))
                .andExpect(jsonPath("$.vehicles[0].entryTime").value(WebFixtures.ENTRY.toEpochMilli()));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void get_missing_returns404() throws Exception {
        when(manageParkings.getParking("nope")).thenThrow(new ParkingNotFoundException());

        mockMvc.perform(get("/parkings/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("El parking no existe"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void create_returnsParkingWithoutIds() throws Exception {
        when(manageParkings.createParking(COMMAND)).thenReturn(WebFixtures.parking());

        mockMvc.perform(post("/parkings/").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.name").value("Parking Centro"))
                .andExpect(jsonPath("$.owner.id").doesNotExist())
                .andExpect(jsonPath("$.owner.username").value("socio"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void create_unknownOwner_returns404() throws Exception {
        when(manageParkings.createParking(any())).thenThrow(new UserNotFoundException());

        mockMvc.perform(post("/parkings/").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Usuario referenciado no encontrado"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void create_invalid_returns400() throws Exception {
        mockMvc.perform(post("/parkings/").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"abc\",\"address\":\"short\",\"capacity\":0,\"costPerHour\":-1,\"idOwner\":\"\"}"))
                .andExpect(status().isBadRequest());
        verify(manageParkings, never()).createParking(any());
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void create_asPartner_forbidden() throws Exception {
        mockMvc.perform(post("/parkings/").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verify(manageParkings, never()).createParking(any());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void update_returnsParkingWithIds() throws Exception {
        Parking updated = WebFixtures.parking();
        when(manageParkings.updateParking(eq("park-1"), eq(COMMAND))).thenReturn(updated);

        mockMvc.perform(put("/parkings/park-1").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("park-1"))
                .andExpect(jsonPath("$.owner.id").value("owner-1"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void delete_returnsMessage() throws Exception {
        mockMvc.perform(delete("/parkings/park-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Parking eliminado exitosamente"));
        verify(manageParkings).deleteParking("park-1");
    }

    @Test
    void anonymous_unauthorized() throws Exception {
        mockMvc.perform(get("/parkings/"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }
}
