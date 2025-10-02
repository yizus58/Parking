package com.nelumbo.park.service;

import com.nelumbo.park.dto.response.MonthParkingStatsResponse;
import com.nelumbo.park.dto.response.MonthPartnerStatsResponse;
import com.nelumbo.park.dto.response.WeeklyParkingStatsResponse;
import com.nelumbo.park.dto.response.WeeklyPartnerStatsResponse;
import com.nelumbo.park.exception.exceptions.LimitParkingFullException;
import com.nelumbo.park.exception.exceptions.ParkingNotFoundException;
import com.nelumbo.park.exception.exceptions.VehicleOutParkingException;
import com.nelumbo.park.dto.request.VehicleCreateRequest;
import com.nelumbo.park.dto.request.VehicleUpdateRequest;
import com.nelumbo.park.dto.response.VehicleCreateResponse;
import com.nelumbo.park.dto.response.VehicleExitResponse;
import com.nelumbo.park.dto.response.IndicatorResponse;
import com.nelumbo.park.dto.response.TopVehicleResponse;
import com.nelumbo.park.entity.Vehicle;
import com.nelumbo.park.entity.User;
import com.nelumbo.park.entity.Parking;
import com.nelumbo.park.exception.exceptions.InsufficientPermissionsException;
import com.nelumbo.park.exception.exceptions.VehicleNotFoundException;
import com.nelumbo.park.exception.exceptions.VehicleAlreadyInParkingException;
import com.nelumbo.park.enums.VehicleStatus;
import com.nelumbo.park.mapper.VehicleMapper;
import com.nelumbo.park.repository.VehicleRepository;
import com.nelumbo.park.repository.UserRepository;
import com.nelumbo.park.service.infrastructure.SecurityService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;
    private final SecurityService securityService;
    private final UserRepository userRepository;
    private final ParkingStatsService parkingStatsService;

    public VehicleService(
            VehicleRepository vehicleRepository,
            VehicleMapper vehicleMapper,
            SecurityService securityService,
            UserRepository userRepository,
            ParkingStatsService parkingStatsService
    ) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleMapper = vehicleMapper;
        this.securityService = securityService;
        this.userRepository = userRepository;
        this.parkingStatsService = parkingStatsService;
    }

    public List<Vehicle> getAllVehicles() {
        User currentUser = securityService.getCurrentUser();

        if (securityService.isAdmin()) {
            return vehicleRepository.findAll();
        } else if (securityService.isSocio()) {
            return vehicleRepository.findByAdmin(currentUser);
        }

        return vehicleRepository.findAll();
    }

    public Vehicle getVehicleById(String id) {
        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow(VehicleNotFoundException::new);

        User currentUser = securityService.getCurrentUser();

        if (securityService.isSocio() && !vehicle.getAdmin().getId().equals(currentUser.getId())) {
            throw new InsufficientPermissionsException();
        }

        return vehicle;
    }

    public VehicleCreateResponse createVehicle(VehicleCreateRequest vehicleCreateRequest) {
        Optional<Vehicle> existingVehicleOpt = vehicleRepository.findByPlateNumberAndStatus(vehicleCreateRequest.getPlateNumber(), VehicleStatus.IN);
        if (existingVehicleOpt.isPresent()) {
            throw new VehicleAlreadyInParkingException(
                    "El vehiculo con placa " + vehicleCreateRequest.getPlateNumber() +
                            " ya está registrado y actualmente en un parking"
            );
        }

        Vehicle vehicle = vehicleMapper.toEntity(vehicleCreateRequest);

        User currentUser = securityService.getCurrentUser();
        String userId = currentUser.getId();

        if (vehicle.getParking() == null) {
            throw new ParkingNotFoundException();
        }

        Boolean validateUserParking = vehicle.getParking().getOwner().getId().equals(userId);
        if (Boolean.FALSE.equals(validateUserParking)) {
            throw new InsufficientPermissionsException();
        }

        Boolean validateLimitParking = validateLimitParking(vehicle.getParking().getId());
        if (Boolean.TRUE.equals(validateLimitParking)) {
            throw new LimitParkingFullException(
                    "El limite de vehiculos en parking "
                            + vehicle.getParking().getName() +
                            " ya ha sido alcanzado"
            );
        }

        vehicle.setCostPerHour(vehicle.getParking().getCostPerHour());

        User admin = userRepository.findById(userId).orElse(null);
        Date entryTime = new Date();

        vehicle.setEntryTime(entryTime);
        vehicle.setAdmin(admin);
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return vehicleMapper.toSimpleResponse(savedVehicle);
    }

    public Boolean validateLimitParking(String idParking) {
        List<Object[]> limitParking = vehicleRepository.findLimitParking(idParking, VehicleStatus.IN);
        if (limitParking.isEmpty() || limitParking.getFirst().length < 2) {
            return false;
        }
        Object[] data = limitParking.getFirst();
        long vehicleCount = ((Number) data[0]).longValue();
        long vehicleLimit = ((Number) data[1]).longValue();
        return vehicleCount >= vehicleLimit;
    }

    public VehicleExitResponse exitVehicle(VehicleUpdateRequest vehicleUpdateRequest) {
        String vehiclePlate = vehicleUpdateRequest.getPlateNumber().toUpperCase();
        Vehicle existingVehicle = vehicleRepository.findByPlateNumberAndStatus(vehiclePlate, VehicleStatus.IN)
                .orElseThrow(VehicleNotFoundException::new);

        User currentUser = securityService.getCurrentUser();

        if (securityService.isSocio() && !existingVehicle.getAdmin().getId().equals(currentUser.getId())) {
            throw new InsufficientPermissionsException();
        }

        Parking parking = existingVehicle.getParking();
        if (parking == null) {
            throw new ParkingNotFoundException();
        }

        if (existingVehicle.getStatus() != VehicleStatus.IN) {
            throw new VehicleOutParkingException();
        }

        Date exitTime = new Date();

        Float costParking = existingVehicle.getCostPerHour();
        Date entryTime = existingVehicle.getEntryTime();

        long timeDifference = Math.abs(exitTime.getTime() - entryTime.getTime());
        long minutesParked = timeDifference / (1000 * 60);
        long hoursParked = (long) Math.ceil((double) minutesParked / 60);
        Float totalCost = hoursParked * costParking;

        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));
        String formattedCost = currencyFormatter.format(totalCost).replace("COP", "$").replace(",00", "");

        existingVehicle.setExitTime(exitTime);
        existingVehicle.setStatus(VehicleStatus.OUT);
        Vehicle savedVehicle = vehicleRepository.save(existingVehicle);

        return vehicleMapper.toExitResponse(savedVehicle, entryTime, exitTime, formattedCost);
    }

    public void deleteVehicle(String id) {
        Vehicle existingVehicle = vehicleRepository.findById(id).orElseThrow(VehicleNotFoundException::new);

        vehicleRepository.delete(existingVehicle);
    }

    public List<IndicatorResponse> getFirstTimeParkedVehicles() {
        List<Vehicle> firstTimeVehicles = vehicleRepository.findFirstTimeParkedVehicles(VehicleStatus.IN);

        return firstTimeVehicles.stream()
                .map(vehicle -> new IndicatorResponse(
                        vehicle.getPlateNumber(),
                        vehicle.getModel(),
                        vehicle.getEntryTime(),
                        vehicle.getExitTime(),
                        new IndicatorResponse.ParkingSimpleResponse(vehicle.getParking().getName())
                ))
                .toList();
    }

    public List<TopVehicleResponse> getTopVehicles() {
        Pageable topTen = PageRequest.of(0, 10);
        List<Object[]> topVehiclesData = vehicleRepository.findTopVehiclesByVisits(topTen);

        return topVehiclesData.stream()
                .map(data -> new TopVehicleResponse(
                        (String) data[0],
                        ((Number) data[1]).longValue()
                ))
                .toList();
    }

    public List<TopVehicleResponse> getTopVehicleById(String id) {
        Pageable topTen = PageRequest.of(0, 10);
        List<Object[]> topVehiclesData = vehicleRepository.findTopVehicleById(id, topTen);

        if (topVehiclesData.isEmpty()) {
            throw new ParkingNotFoundException();
        }

        return topVehiclesData.stream()
                .map(data -> new TopVehicleResponse(
                        (String) data[0],
                        ((Number) data[1]).longValue()
                ))
                .toList();
    }

    public WeeklyPartnerStatsResponse getPartnersRanking() {
        return parkingStatsService.getPartnersRanking();
    }

    public MonthPartnerStatsResponse getPartnersRankingMonth() {
        return parkingStatsService.getPartnersRankingMonth();
    }

    public WeeklyParkingStatsResponse getParkingRanking() {
        return parkingStatsService.getParkingRanking();
    }

    public MonthParkingStatsResponse getParkingRankingMonth() {
        return parkingStatsService.getParkingRankingMonth();
    }
}
