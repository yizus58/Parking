package com.nelumbo.park.service.infrastructure;

import com.nelumbo.park.dto.response.FileInfoResponse;
import com.nelumbo.park.dto.response.FileUploadResultResponse;
import com.nelumbo.park.dto.response.RabbitMQResponse;
import com.nelumbo.park.dto.response.VehicleOutDetailResponse;
import com.nelumbo.park.dto.response.EmailAttachmentResponse;
import com.nelumbo.park.dto.response.EmailDataResponse;
import com.nelumbo.park.service.VehicleReportService;
import com.nelumbo.park.utils.Excel;
import com.nelumbo.park.utils.ExcelComponent;
import com.nelumbo.park.utils.HtmlGenerator;
import com.nelumbo.park.utils.Pdf;
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
    private final Pdf pdf;
    private final S3Service s3Service;
    private final RabbitMQService rabbitMQService;

    private final List<FileUploadResultResponse> uploadedFiles = new ArrayList<>();

    @Value("${app.subject}")
    private String subject;

    @Value("${type.message}")
    private String typeMessage;

    @Value("${spring.mvc.contentnegotiation.media-types.pdf}")
    private String pdfContentType;

    public CronService(
            VehicleReportService vehicleReportService,
            Excel excel,
            ExcelComponent excelGenerator,
            Pdf pdf,
            S3Service s3Service,
            RabbitMQService rabbitMQService
    ) {
        this.vehicleReportService = vehicleReportService;
        this.excel = excel;
        this.excelGenerator = excelGenerator;
        this.pdf = pdf;
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
            // Excel generation
            FileInfoResponse excelFileInfo = generateFileNames(vehicleOutDetailResponse);
            String excelContentType = getContentType();
            byte[] excelBuffer = this.excel.generarExcelPorUsuario(List.of(vehicleOutDetailResponse));

            // PDF generation
            String pdfFileName = excelFileInfo.getNameFile().replace(".xlsx", ".pdf");
            String pdfS3Name = UUID.randomUUID().toString().replace("-", "");
            FileInfoResponse pdfFileInfo = new FileInfoResponse(pdfFileName, pdfS3Name);
            byte[] pdfBuffer = this.pdf.generarPdfPorUsuario(List.of(vehicleOutDetailResponse));

            List<EmailAttachmentResponse> attachments = new ArrayList<>();

            // Upload Excel
            try {
                Map<String, String> uploadResult = this.s3Service.uploadFile(excelBuffer, excelContentType, excelFileInfo.getS3Name());
                if (uploadResult != null && uploadResult.containsKey("Key")) {
                    attachments.add(new EmailAttachmentResponse(excelFileInfo.getNameFile(), excelFileInfo.getS3Name()));
                    addToUploadedFiles(vehicleOutDetailResponse, excelFileInfo);
                }
            } catch (Exception e) {
                log.error("Error subiendo el archivo Excel para el usuario {}: {}", vehicleOutDetailResponse.getUserId(), e.getMessage());
            }

            // Upload PDF
            try {
                Map<String, String> uploadResultPdf = this.s3Service.uploadFile(pdfBuffer, this.pdfContentType, pdfFileInfo.getS3Name());
                if (uploadResultPdf != null && uploadResultPdf.containsKey("Key")) {
                    attachments.add(new EmailAttachmentResponse(pdfFileInfo.getNameFile(), pdfFileInfo.getS3Name()));
                    addToUploadedFiles(vehicleOutDetailResponse, pdfFileInfo);
                }
            } catch (Exception e) {
                log.error("Error subiendo el archivo PDF para el usuario {}: {}", vehicleOutDetailResponse.getUserId(), e.getMessage());
            }

            if (!attachments.isEmpty()) {
                sendEmailWithAttachments(vehicleOutDetailResponse, attachments);
            }

        } catch (IOException e) {
            log.error("Error generando archivo para usuario {}: {}",
                    vehicleOutDetailResponse.getUserId(), e.getMessage());
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

    private void sendEmailWithAttachments(VehicleOutDetailResponse vehicleOutDetailResponse, List<EmailAttachmentResponse> attachments) {
        try {
            String email = Optional.ofNullable(vehicleOutDetailResponse.getEmail()).orElse("");
            String htmlContent = HtmlGenerator.generateHtmlContent(vehicleOutDetailResponse);

            EmailDataResponse data = new EmailDataResponse(email, htmlContent, subject, attachments);
            RabbitMQResponse response = new RabbitMQResponse(typeMessage, data);
            this.rabbitMQService.publishMessageBackoff(response);
        } catch (Exception e) {
            log.error("Error enviando notificación por email para usuario {}: {}",
                    vehicleOutDetailResponse.getUserId(), e.getMessage());
        }
    }

    private void addToUploadedFiles(VehicleOutDetailResponse vehicleOutDetailResponse, FileInfoResponse fileInfo) {
        String userId = vehicleOutDetailResponse.getUserId();

        Optional<FileUploadResultResponse> existingUploadInfo = uploadedFiles.stream()
                .filter(r -> r.getIdUser().equals(userId))
                .findFirst();

        if (existingUploadInfo.isPresent()) {
            existingUploadInfo.get().getFiles().add(fileInfo);
        } else {
            String email = Optional.ofNullable(vehicleOutDetailResponse.getEmail()).orElse("");
            List<FileInfoResponse> files = new ArrayList<>();
            files.add(fileInfo);
            FileUploadResultResponse newUploadInfo = new FileUploadResultResponse(userId, email, files);
            uploadedFiles.add(newUploadInfo);
        }
    }
}
