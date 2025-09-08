package com.nelumbo.park.service;

import com.nelumbo.park.dto.FileUploadResult;
import com.nelumbo.park.dto.response.VehicleOutDetailResponse;
import com.nelumbo.park.utils.Excel;
import com.nelumbo.park.utils.ExcelComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class CronService {

    private final VehicleReportService vehicleReportService;
    private final Excel excel;
    private final ExcelComponent excelGenerator;
    private final S3Service s3Service;

    private final List<FileUploadResult> uploadedFiles = new ArrayList<>();

    public CronService(
            VehicleReportService vehicleReportService,
            Excel excel,
            ExcelComponent excelGenerator,
            S3Service s3Service
    ) {
        this.vehicleReportService = vehicleReportService;
        this.excel = excel;
        this.excelGenerator = excelGenerator;
        this.s3Service = s3Service;
    }

    public boolean runDailyTask() {
        List<VehicleOutDetailResponse> dataVehicleOutParking = this.vehicleReportService.getVehiclesOutDetails();
        if (dataVehicleOutParking == null || dataVehicleOutParking.isEmpty()) {
            return false;
        }

        uploadedFiles.clear();

        for (VehicleOutDetailResponse vehicleOutDetailResponse : dataVehicleOutParking) {
            try {
                String rawName = Optional.ofNullable(vehicleOutDetailResponse.getParking()).orElse("diario");
                String safeName = rawName.replaceAll("[^\\w\\-]+", "_");
                String userId = vehicleOutDetailResponse.getUserId();
                String shortId = userId != null && userId.length() >= 8 ? userId.substring(0, 8) : "undefined";
                safeName = safeName.length() > 50 ? safeName.substring(0, 50) : safeName;
                String dateGenerate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                String fileName = String.format("reporte_%s_%s_%s.xlsx", safeName, shortId, dateGenerate).toLowerCase();
                String nameS3 = UUID.randomUUID().toString().replace("-", "");
                String email = Optional.ofNullable(vehicleOutDetailResponse.getEmail()).orElse("");
                String contentType = this.excelGenerator.getContentType();

                if (contentType == null || contentType.isEmpty()) {
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                }

                byte[] buffer = this.excel.generarExcelPorUsuario(List.of(vehicleOutDetailResponse));

                Map<String, String> uploadResult = null;
                try {
                    uploadResult = this.s3Service.uploadFile(buffer, contentType, nameS3);
                } catch (Exception e) {
                    log.warn("Error en subida normal para archivo {}, intentando subida directa: {}", nameS3, e.getMessage());
                }

                if (uploadResult != null && uploadResult.containsKey("Key")) {
                    // Se implementara el rabbitMQ tras crear el html para el envio
                }

            } catch (IOException e) {
                log.error("Error procesando archivo para usuario {}: {}", 
                        vehicleOutDetailResponse.getUserId(), e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                log.error("Error inesperado procesando archivo para usuario {}: {}", 
                        vehicleOutDetailResponse.getUserId(), e.getMessage());
                e.printStackTrace();
            }
        }
        log.info("Proceso completado. Total de archivos subidos: {}", uploadedFiles.size());
        return true;
    }
}
