package com.nelumbo.park.infrastructure.adapter.in.web;

import com.nelumbo.park.application.port.in.ParkingIndicatorsUseCase;
import com.nelumbo.park.domain.exception.ParkingNotFoundException;
import com.nelumbo.park.domain.model.stats.ParkingRanking;
import com.nelumbo.park.domain.model.stats.PartnerRanking;
import com.nelumbo.park.domain.model.stats.RankingPeriod;
import com.nelumbo.park.domain.model.stats.TopParking;
import com.nelumbo.park.domain.model.stats.TopPartner;
import com.nelumbo.park.domain.model.stats.TopVehicle;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {IndicatorController.class, RankingController.class,
        ParkingRankingController.class, PartnersRankingController.class})
class IndicatorsAndRankingsControllerTest extends WebLayerTestSupport {

    private static final RankingPeriod WEEK = RankingPeriod.weekOf(LocalDateTime.of(2026, 11, 20, 9, 0));
    private static final RankingPeriod MONTH = RankingPeriod.monthOf(LocalDateTime.of(2026, 11, 20, 9, 0));

    @MockitoBean
    private ParkingIndicatorsUseCase indicators;

    @Test
    @WithMockUser(authorities = "SOCIO")
    void firstTimeParked() throws Exception {
        when(indicators.firstTimeParkedVehicles()).thenReturn(List.of(WebFixtures.inside()));

        mockMvc.perform(get("/indicators/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].plateNumber").value("ABC123"))
                .andExpect(jsonPath("$[0].modelVehicle").value("Mazda"))
                .andExpect(jsonPath("$[0].entryTime").value("20-11-2026 09:15"))
                .andExpect(jsonPath("$[0].parking.name").value("Parking Centro"));
    }

    @Test
    void indicators_anonymous_unauthorized() throws Exception {
        mockMvc.perform(get("/indicators/")).andExpect(status().isUnauthorized());
        verifyNoInteractions(indicators);
    }

    @Test
    @WithMockUser(authorities = "OTHER")
    void indicators_unknownRole_forbidden() throws Exception {
        mockMvc.perform(get("/indicators/")).andExpect(status().isForbidden());
        verifyNoInteractions(indicators);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void topVehicles() throws Exception {
        when(indicators.topVehicles()).thenReturn(List.of(new TopVehicle("ABC123", 7L)));

        mockMvc.perform(get("/rankings/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].plateNumber").value("ABC123"))
                .andExpect(jsonPath("$[0].totalVisits").value(7));
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void topVehiclesByParking_unknown_returns404() throws Exception {
        when(indicators.topVehiclesByParking("nope")).thenThrow(new ParkingNotFoundException());
        mockMvc.perform(get("/rankings/nope")).andExpect(status().isNotFound());
    }

    @Test
    void rankings_anonymous_unauthorized() throws Exception {
        mockMvc.perform(get("/rankings/")).andExpect(status().isUnauthorized());
        verifyNoInteractions(indicators);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void partnersWeek() throws Exception {
        when(indicators.partnersRankingOfWeek())
                .thenReturn(new PartnerRanking(WEEK, List.of(new TopPartner("socio", 12L, "park-1"))));

        mockMvc.perform(get("/partners-rankings/week"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value("2026-11-16T05:00:00"))
                .andExpect(jsonPath("$.weekEnd").value("2026-11-22T23:59:59"))
                .andExpect(jsonPath("$.topPartners[0].partnerName").value("socio"))
                .andExpect(jsonPath("$.topPartners[0].vehicleCount").value(12))
                .andExpect(jsonPath("$.topPartners[0].parkingId").value("park-1"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void partnersMonth() throws Exception {
        when(indicators.partnersRankingOfMonth()).thenReturn(new PartnerRanking(MONTH, List.of()));

        mockMvc.perform(get("/partners-rankings/month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthStart").value("2026-11-01T05:00:00"))
                .andExpect(jsonPath("$.topPartners").isEmpty());
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void partners_asPartner_forbidden() throws Exception {
        mockMvc.perform(get("/partners-rankings/week")).andExpect(status().isForbidden());
        verifyNoInteractions(indicators);
    }

    @Test
    void partners_anonymous_unauthorized() throws Exception {
        mockMvc.perform(get("/partners-rankings/week")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void parkingWeek_keepsLegacyCostFormat() throws Exception {
        when(indicators.parkingRankingOfWeek())
                .thenReturn(new ParkingRanking(WEEK, List.of(new TopParking("park-1", "Parking Centro", 3L, 7500f))));

        mockMvc.perform(get("/parking-rankings/week"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value("2026-11-16T05:00:00"))
                .andExpect(jsonPath("$.topParking[0].parkingId").value("park-1"))
                .andExpect(jsonPath("$.topParking[0].parkingName").value("Parking Centro"))
                .andExpect(jsonPath("$.topParking[0].totalVehicles").value(3))
                .andExpect(jsonPath("$.topParking[0].totalCost").value(String.format("%.1f", 7500f) + "f"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void parkingMonth() throws Exception {
        when(indicators.parkingRankingOfMonth()).thenReturn(new ParkingRanking(WEEK, List.of()));

        mockMvc.perform(get("/parking-rankings/month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthStart").value("2026-11-16T05:00:00"));
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void parkingRanking_asPartner_forbidden() throws Exception {
        mockMvc.perform(get("/parking-rankings/week")).andExpect(status().isForbidden());
        verifyNoInteractions(indicators);
    }
}
