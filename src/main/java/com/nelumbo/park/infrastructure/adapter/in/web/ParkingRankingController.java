package com.nelumbo.park.infrastructure.adapter.in.web;

import com.nelumbo.park.application.port.in.ParkingIndicatorsUseCase;
import com.nelumbo.park.infrastructure.adapter.in.web.mapper.WebMapper;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.MonthParkingStatsResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.WeeklyParkingStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/parking-rankings")
@Tag(name = "Parking Rankings", description = "Parking Ranking API")
public class ParkingRankingController {

    private final ParkingIndicatorsUseCase indicators;

    public ParkingRankingController(ParkingIndicatorsUseCase indicators) {
        this.indicators = indicators;
    }

    @Operation(summary = "Obtiene los top 3 de parqueaderos con mayor ganancia")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking encontrado",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WeeklyParkingStatsResponse.class))),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta accion", content = @Content)
    })
    @GetMapping("/week")
    @PreAuthorize("hasAuthority('ADMIN')")
    public WeeklyParkingStatsResponse getParkingRanking() {
        return WebMapper.toWeeklyParkingStats(indicators.parkingRankingOfWeek());
    }

    @Operation(summary = "Obtiene los top 3 de parqueaderos con mayor ganancia")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WeeklyParkingStatsResponse.class))),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta accion", content = @Content)
    })
    @GetMapping("/month")
    @PreAuthorize("hasAuthority('ADMIN')")
    public MonthParkingStatsResponse getParkingRankingMonth() {
        return WebMapper.toMonthParkingStats(indicators.parkingRankingOfMonth());
    }
}
