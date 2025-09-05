package com.nelumbo.park.service;

import com.nelumbo.park.dto.response.VehicleOutDetailResponse;
import com.nelumbo.park.utils.Excel;
import com.nelumbo.park.utils.ExcelComponent;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CronService {

    private final VehicleReportService vehicleReportService;
    private final Excel excel;
    private final ExcelComponent excelGenerator;

    public CronService(
            VehicleReportService vehicleReportService,
            Excel excel,
            ExcelComponent excelGenerator
    ) {
        this.vehicleReportService = vehicleReportService;
        this.excel = excel;
        this.excelGenerator = excelGenerator;
    }

    public boolean runDailyTask() {
        List<VehicleOutDetailResponse> dataVehicleOutParking = this.vehicleReportService.getVehiclesOutDetails();
        if (dataVehicleOutParking == null || dataVehicleOutParking.isEmpty()) {
            return false;
        }

        for (VehicleOutDetailResponse vehicleOutDetailResponse : dataVehicleOutParking) {
            try {
                String rawName = Optional.ofNullable(vehicleOutDetailResponse.getParking()).orElse("diario");
                String safeName = rawName.replaceAll("[^\\w\\-]+", "_");
                String userId = vehicleOutDetailResponse.getUserId();
                String shortId = userId != null && userId.length() >= 8 ? userId.substring(0, 8) : "undefined";
                safeName = safeName.length() > 50 ? safeName.substring(0, 50) : safeName;
                String dateGenerate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                String filename = String.format("reporte_%s_%s_%s.xlsx", safeName, shortId, dateGenerate).toLowerCase();
                //String nameS3 = UUID.randomUUID().toString().replace("-", ""); // Generar el nombre de archivo aleatorio para S3

                String contentType = this.excelGenerator.getContentType();

                if (contentType == null || contentType.isEmpty()) {
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                }

                byte[] buffer = this.excel.generarExcelPorUsuario(List.of(vehicleOutDetailResponse));

                // Guardar en disco, luego se reemplazara con s3
                Path outputPath = Paths.get("C:/Users/NelumboDev/Desktop/reportes/", filename);
                Files.write(outputPath, buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }
}
