package com.nelumbo.park.infrastructure.adapter.in.web;

import com.nelumbo.park.application.port.in.ParkingIndicatorsUseCase;
import com.nelumbo.park.infrastructure.adapter.in.web.mapper.WebMapper;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.MonthPartnerStatsResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.WeeklyPartnerStatsResponse;
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
@RequestMapping("/partners-rankings")
@Tag(name = "Partners Rankings", description = "Partners Ranking API")
public class PartnersRankingController {
    
    private final ParkingIndicatorsUseCase indicators;
    
    public PartnersRankingController(ParkingIndicatorsUseCase indicators) {
        this.indicators = indicators;
    }

    @Operation(summary = "Obtiene los top 3 de partners con mayor ganancia en la semana")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking encontrado",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WeeklyPartnerStatsResponse.class))),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta accion", content = @Content)
    })
    @GetMapping("/week")
    @PreAuthorize("hasAuthority('ADMIN')")
    public WeeklyPartnerStatsResponse getPartnersRanking() {
        return WebMapper.toWeeklyPartnerStats(indicators.partnersRankingOfWeek());
    }

    @Operation(summary = "Obtiene los top 3 de partners con mayor ganancia en el mes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ranking encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MonthPartnerStatsResponse.class))),
            @ApiResponse(responseCode = "401", description = "No tienes permisos para realizar esta accion", content = @Content)
    })
    @GetMapping("/month")
    @PreAuthorize("hasAuthority('ADMIN')")
    public MonthPartnerStatsResponse getPartnersRankingMonth() {
        return WebMapper.toMonthPartnerStats(indicators.partnersRankingOfMonth());
    }
}
