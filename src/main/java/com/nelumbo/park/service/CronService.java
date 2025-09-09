package com.nelumbo.park.service;

import com.nelumbo.park.dto.response.FileInfoResponse;
import com.nelumbo.park.dto.response.FileUploadResultResponse;
import com.nelumbo.park.dto.response.RabbitMQResponse;
import com.nelumbo.park.dto.response.VehicleOutDetailResponse;
import com.nelumbo.park.dto.response.EmailAttachmentResponse;
import com.nelumbo.park.dto.response.EmailDataResponse;
import com.nelumbo.park.utils.Excel;
import com.nelumbo.park.utils.ExcelComponent;
import com.nelumbo.park.utils.HtmlGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final RabbitMQService rabbitMQService;

    private final List<FileUploadResultResponse> uploadedFiles = new ArrayList<>();

    @Value("${app.subject}")
    private String subject;

    @Value("${type.message}")
    private String typeMessage;

    public CronService(
            VehicleReportService vehicleReportService,
            Excel excel,
            ExcelComponent excelGenerator,
            S3Service s3Service,
            RabbitMQService rabbitMQService
    ) {
        this.vehicleReportService = vehicleReportService;
        this.excel = excel;
        this.excelGenerator = excelGenerator;
        this.s3Service = s3Service;
        this.rabbitMQService = rabbitMQService;
    }

    public boolean runDailyTask() {
        List<VehicleOutDetailResponse> dataVehicleOutParking = this.vehicleReportService.getVehiclesOutDetails();
        if (dataVehicleOutParking == null || dataVehicleOutParking.isEmpty()) {
            return false;
        }

        uploadedFiles.clear();

        for (VehicleOutDetailResponse vehicleOutDetailResponse : dataVehicleOutParking) {
            processVehicleReport(vehicleOutDetailResponse);
        }

        log.info("Proceso completado. Total de archivos subidos: {}", uploadedFiles.size());
        return true;
    }

    private void processVehicleReport(VehicleOutDetailResponse vehicleOutDetailResponse) {
        try {
            FileInfoResponse fileNameInfo = generateFileNames(vehicleOutDetailResponse);
            String contentType = getContentType();
            byte[] buffer = this.excel.generarExcelPorUsuario(List.of(vehicleOutDetailResponse));

            if (uploadFileAndSendEmail(vehicleOutDetailResponse, fileNameInfo, buffer, contentType)) {
                addToUploadedFiles(vehicleOutDetailResponse, fileNameInfo);
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

    private FileInfoResponse generateFileNames(VehicleOutDetailResponse vehicleOutDetailResponse) {
        String rawName = Optional.ofNullable(vehicleOutDetailResponse.getParking()).orElse("diario");
        String safeName = rawName.replaceAll("[^\\w\\-]+", "_");
        String userId = vehicleOutDetailResponse.getUserId();
        String shortId = userId != null && userId.length() >= 8 ? userId.substring(0, 8) : "undefined";

        safeName = safeName.length() > 50 ? safeName.substring(0, 50) : safeName;
        String dateGenerate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fileName = String.format("reporte_%s_%s_%s.xlsx", safeName, shortId, dateGenerate).toLowerCase();
        String nameS3 = UUID.randomUUID().toString().replace("-", "");

        return new FileInfoResponse(fileName, nameS3);
    }

    private String getContentType() {
        String contentType = this.excelGenerator.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        return contentType;
    }

    private boolean uploadFileAndSendEmail(VehicleOutDetailResponse vehicleOutDetailResponse,
                                           FileInfoResponse fileNameInfo, byte[] buffer, String contentType) {
        Map<String, String> uploadResult = null;
        try {
            uploadResult = this.s3Service.uploadFile(buffer, contentType, fileNameInfo.getS3Name());
        } catch (Exception e) {
            log.warn("Error en subida normal para archivo {}, intentando subida directa: {}", 
                    fileNameInfo.getS3Name(), e.getMessage());
        }

        if (uploadResult != null && uploadResult.containsKey("Key")) {
            sendEmailNotification(vehicleOutDetailResponse, fileNameInfo);
            return true;
        }
        return false;
    }

    private void sendEmailNotification(VehicleOutDetailResponse vehicleOutDetailResponse, FileInfoResponse fileNameInfo) {
        try {
            String email = Optional.ofNullable(vehicleOutDetailResponse.getEmail()).orElse("");
            String htmlContent = HtmlGenerator.generateHtmlContent(vehicleOutDetailResponse);

            EmailAttachmentResponse attachment = new EmailAttachmentResponse(fileNameInfo.getNameFile(), fileNameInfo.getS3Name());
            EmailDataResponse data = new EmailDataResponse(email, htmlContent, subject, List.of(attachment));
            RabbitMQResponse response = new RabbitMQResponse(typeMessage, data);
            this.rabbitMQService.publishMessageBackoff(response);
        } catch (Exception e) {
            log.error("Error enviando notificación por email para usuario {}: {}", 
                    vehicleOutDetailResponse.getUserId(), e.getMessage());
        }
    }

    private void addToUploadedFiles(VehicleOutDetailResponse vehicleOutDetailResponse, FileInfoResponse fileNameInfo) {
        String userId = vehicleOutDetailResponse.getUserId();
        String email = Optional.ofNullable(vehicleOutDetailResponse.getEmail()).orElse("");
        FileInfoResponse fileInfo = new FileInfoResponse(fileNameInfo.getNameFile(), fileNameInfo.getS3Name());
        FileUploadResultResponse uploadInfo = new FileUploadResultResponse(userId, email, fileInfo);
        uploadedFiles.add(uploadInfo);
    }
}
