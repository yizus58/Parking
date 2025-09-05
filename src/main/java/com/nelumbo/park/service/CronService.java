package com.nelumbo.park.service;

import com.nelumbo.park.dto.response.VehicleOutDetailResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CronService {

    private final VehicleReportService vehicleReportService;

    public CronService(
            VehicleReportService vehicleReportService
    ) {
        this.vehicleReportService = vehicleReportService;
    }

    public boolean runDailyTask() {
        List<VehicleOutDetailResponse> dataVehicleOutParking = this.vehicleReportService.getVehiclesOutDetails();
        if (dataVehicleOutParking == null || dataVehicleOutParking.isEmpty()) {
            return false;
        }

        for (VehicleOutDetailResponse vehicleOutDetailResponse : dataVehicleOutParking) {
            // Codigo para generar el Excel
        }

        return true;
    }

    public boolean executeReportTask() {
        System.out.println("Ejecutando tarea de reporte...");
        return true;
    }
}
