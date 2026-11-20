package com.nelumbo.park.infrastructure.adapter.in.web.mapper;

import com.nelumbo.park.application.port.in.LoginUseCase.AuthenticatedUser;
import com.nelumbo.park.application.port.in.ManageParkingsUseCase.ParkingDetails;
import com.nelumbo.park.application.port.in.RegisterVehicleExitUseCase.VehicleExit;
import com.nelumbo.park.domain.model.Parking;
import com.nelumbo.park.domain.model.User;
import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.domain.model.stats.ParkingRanking;
import com.nelumbo.park.domain.model.stats.PartnerRanking;
import com.nelumbo.park.domain.model.stats.TopParking;
import com.nelumbo.park.domain.model.stats.TopPartner;
import com.nelumbo.park.domain.model.stats.TopVehicle;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.IndicatorResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.MonthParkingStatsResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.MonthPartnerStatsResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.ParkingResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.ParkingWithVehiclesResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.TopParkingResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.TopPartnerResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.TopVehicleResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.UserLoginResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.UserResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.VehicleCreateResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.VehicleExitResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.VehicleResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.VehicleSimpleResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.WeeklyParkingStatsResponse;
import com.nelumbo.park.infrastructure.adapter.in.web.dto.response.WeeklyPartnerStatsResponse;

import java.text.NumberFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Domain to REST DTO conversions. Pure functions; the JSON produced is the same
 * as the previous MapStruct mappers.
 */
public final class WebMapper {

    private static final Locale COLOMBIA = Locale.of("es", "CO");

    private WebMapper() {
    }

    // ---- users -------------------------------------------------------------

    public static UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    private static UserResponse toUserResponseWithoutId(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(null, user.getUsername(), user.getEmail(), user.getRole());
    }

    public static UserLoginResponse toLoginResponse(AuthenticatedUser authenticated) {
        User user = authenticated.user();
        return new UserLoginResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                "Bearer", authenticated.accessToken());
    }

    // ---- parkings ----------------------------------------------------------

    /** Parking with id and owner (owner includes its id). */
    public static ParkingResponse toParkingResponse(Parking parking) {
        if (parking == null) {
            return null;
        }
        return new ParkingResponse(parking.getId(), parking.getName(), parking.getAddress(), parking.getCapacity(),
                parking.getCostPerHour(), toUserResponse(parking.getOwner()));
    }

    /** Response of POST /parkings/: no parking id and owner without id. */
    public static ParkingResponse toCreatedParkingResponse(Parking parking) {
        return new ParkingResponse(null, parking.getName(), parking.getAddress(), parking.getCapacity(),
                parking.getCostPerHour(), toUserResponseWithoutId(parking.getOwner()));
    }

    public static ParkingResponse toParkingResponseWithoutOwner(Parking parking) {
        return new ParkingResponse(parking.getId(), parking.getName(), parking.getAddress(), parking.getCapacity(),
                parking.getCostPerHour(), null);
    }

    public static ParkingWithVehiclesResponse toParkingWithVehicles(ParkingDetails details) {
        Parking parking = details.parking();
        List<VehicleSimpleResponse> vehicles = details.vehicles().stream().map(WebMapper::toVehicleSimple).toList();
        return new ParkingWithVehiclesResponse(parking.getId(), parking.getName(), parking.getAddress(),
                parking.getCapacity(), parking.getCostPerHour(), vehicles);
    }

    // ---- vehicles ----------------------------------------------------------

    public static VehicleResponse toVehicleResponse(Vehicle vehicle) {
        return new VehicleResponse(vehicle.getId(), vehicle.getPlateNumber(), vehicle.getModel(),
                toDate(vehicle.getEntryTime()), toDate(vehicle.getExitTime()), vehicle.getCostPerHour(),
                vehicle.getStatus(), toParkingResponse(vehicle.getParking()), toUserResponse(vehicle.getAdmin()));
    }

    public static VehicleSimpleResponse toVehicleSimple(Vehicle vehicle) {
        return new VehicleSimpleResponse(vehicle.getId(), vehicle.getPlateNumber(), vehicle.getModel(),
                toDate(vehicle.getEntryTime()), toDate(vehicle.getExitTime()), vehicle.getCostPerHour(),
                vehicle.getStatus());
    }

    public static VehicleCreateResponse toVehicleCreateResponse(Vehicle vehicle) {
        return new VehicleCreateResponse(vehicle.getPlateNumber(), vehicle.getModel(), toDate(vehicle.getEntryTime()),
                toDate(vehicle.getExitTime()), vehicle.getCostPerHour(), vehicle.getStatus());
    }

    public static VehicleExitResponse toVehicleExitResponse(VehicleExit exit) {
        Vehicle vehicle = exit.vehicle();
        return new VehicleExitResponse(vehicle.getPlateNumber(), vehicle.getModel(), toDate(vehicle.getEntryTime()),
                toDate(vehicle.getExitTime()), vehicle.getCostPerHour(), vehicle.getStatus(),
                formatPesos(exit.totalCost()));
    }

    /** Same format as before, e.g. "$ 5.000". */
    static String formatPesos(float amount) {
        return NumberFormat.getCurrencyInstance(COLOMBIA).format(amount).replace("COP", "$").replace(",00", "");
    }

    public static IndicatorResponse toIndicator(Vehicle vehicle) {
        return new IndicatorResponse(vehicle.getPlateNumber(), vehicle.getModel(), toDate(vehicle.getEntryTime()),
                toDate(vehicle.getExitTime()),
                new IndicatorResponse.ParkingSimpleResponse(vehicle.getParking().getName()));
    }

    // ---- stats -------------------------------------------------------------

    public static TopVehicleResponse toTopVehicle(TopVehicle top) {
        return new TopVehicleResponse(top.plateNumber(), top.totalVisits());
    }

    public static WeeklyPartnerStatsResponse toWeeklyPartnerStats(PartnerRanking ranking) {
        return new WeeklyPartnerStatsResponse(ranking.period().start(), ranking.period().end(), toTopPartners(ranking));
    }

    public static MonthPartnerStatsResponse toMonthPartnerStats(PartnerRanking ranking) {
        return new MonthPartnerStatsResponse(ranking.period().start(), ranking.period().end(), toTopPartners(ranking));
    }

    public static WeeklyParkingStatsResponse toWeeklyParkingStats(ParkingRanking ranking) {
        return new WeeklyParkingStatsResponse(ranking.period().start(), ranking.period().end(), toTopParkings(ranking));
    }

    public static MonthParkingStatsResponse toMonthParkingStats(ParkingRanking ranking) {
        return new MonthParkingStatsResponse(ranking.period().start(), ranking.period().end(), toTopParkings(ranking));
    }

    private static List<TopPartnerResponse> toTopPartners(PartnerRanking ranking) {
        return ranking.topPartners().stream().map(WebMapper::toTopPartner).toList();
    }

    private static TopPartnerResponse toTopPartner(TopPartner top) {
        return new TopPartnerResponse(top.partnerName(), top.vehicleCount(), top.parkingId());
    }

    private static List<TopParkingResponse> toTopParkings(ParkingRanking ranking) {
        return ranking.topParkings().stream().map(WebMapper::toTopParking).toList();
    }

    /**
     * totalCost keeps its legacy wire format, e.g. "6000.0f", but always with a
     * dot as decimal separator regardless of the server's default locale.
     */
    static TopParkingResponse toTopParking(TopParking top) {
        return new TopParkingResponse(top.parkingId(), top.parkingName(), top.totalVehicles(),
                String.format(Locale.ROOT, "%.1f", top.totalEarnings()) + "f");
    }

    private static Date toDate(Instant instant) {
        return instant != null ? Date.from(instant) : null;
    }
}
