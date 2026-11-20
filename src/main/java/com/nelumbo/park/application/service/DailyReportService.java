package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.in.SendDailyReportsUseCase;
import com.nelumbo.park.application.port.out.EmailNotificationPort;
import com.nelumbo.park.application.port.out.EmailNotificationPort.EmailAttachment;
import com.nelumbo.park.application.port.out.FileStoragePort;
import com.nelumbo.park.application.port.out.ReportRendererPort;
import com.nelumbo.park.application.port.out.VehicleRepositoryPort;
import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.domain.model.VehicleStatus;
import com.nelumbo.park.domain.model.report.VehicleOutDetail;
import com.nelumbo.park.domain.service.DailyReportBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Daily job: one Excel and one PDF per partner and parking with today's exits,
 * uploaded to storage and announced by email. Failures are logged per report
 * so one bad report does not stop the others.
 */
@Slf4j
@Service
public class DailyReportService implements SendDailyReportsUseCase {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final VehicleRepositoryPort vehicleRepository;
    private final ReportRendererPort reportRenderer;
    private final FileStoragePort fileStorage;
    private final EmailNotificationPort emailNotification;
    private final Clock clock;

    public DailyReportService(VehicleRepositoryPort vehicleRepository,
                              ReportRendererPort reportRenderer,
                              FileStoragePort fileStorage,
                              EmailNotificationPort emailNotification,
                              Clock clock) {
        this.vehicleRepository = vehicleRepository;
        this.reportRenderer = reportRenderer;
        this.fileStorage = fileStorage;
        this.emailNotification = emailNotification;
        this.clock = clock;
    }

    @Override
    public boolean sendDailyReports() {
        List<VehicleOutDetail> reports = buildTodayReports();
        if (reports.isEmpty()) {
            return false;
        }
        int uploaded = 0;
        for (VehicleOutDetail report : reports) {
            uploaded += process(report);
        }
        log.info("Proceso completado. Total de archivos subidos: {}", uploaded);
        return true;
    }

    List<VehicleOutDetail> buildTodayReports() {
        LocalDate today = LocalDate.now(clock);
        ZonedDateTime start = today.atStartOfDay(clock.getZone());
        ZonedDateTime end = start.plusDays(1).minusNanos(1_000_000);
        List<Vehicle> exited = vehicleRepository.findExitedBetween(start.toInstant(), end.toInstant()).stream()
                .filter(v -> v.getStatus() == VehicleStatus.OUT)
                .toList();
        return DailyReportBuilder.build(exited);
    }

    private int process(VehicleOutDetail report) {
        try {
            String excelName = fileName(report);
            byte[] excel = reportRenderer.renderExcel(report);
            String pdfName = excelName.replace(".xlsx", ".pdf");
            byte[] pdf = reportRenderer.renderPdf(report);

            List<EmailAttachment> attachments = new ArrayList<>();
            upload(excel, reportRenderer.excelContentType(), excelName, report, attachments, "Excel");
            upload(pdf, reportRenderer.pdfContentType(), pdfName, report, attachments, "PDF");
            if (!attachments.isEmpty()) {
                sendEmail(report, attachments);
            }
            return attachments.size();
        } catch (IOException e) {
            log.error("Error generando archivo para usuario {}: {}", report.getUserId(), e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado procesando archivo para usuario {}: {}", report.getUserId(), e.getMessage());
        }
        return 0;
    }

    private void upload(byte[] content, String contentType, String fileName, VehicleOutDetail report,
                        List<EmailAttachment> attachments, String fileType) {
        String key = UUID.randomUUID().toString().replace("-", "");
        try {
            if (fileStorage.store(content, contentType, key)) {
                attachments.add(new EmailAttachment(fileName, key));
            }
        } catch (Exception e) {
            log.error("Error subiendo el archivo {} para el usuario {}: {}", fileType, report.getUserId(), e.getMessage());
        }
    }

    private void sendEmail(VehicleOutDetail report, List<EmailAttachment> attachments) {
        try {
            String email = Optional.ofNullable(report.getEmail()).orElse("");
            emailNotification.sendEmail(email, reportRenderer.renderEmailHtml(report), attachments);
        } catch (Exception e) {
            log.error("Error enviando notificación por email para usuario {}: {}", report.getUserId(), e.getMessage());
        }
    }

    String fileName(VehicleOutDetail report) {
        String rawName = Optional.ofNullable(report.getParking()).orElse("diario");
        String safeName = rawName.replaceAll("[^\\w\\-]+", "_");
        if (safeName.length() > 50) {
            safeName = safeName.substring(0, 50);
        }
        String userId = report.getUserId();
        String shortId = userId != null && userId.length() >= 8 ? userId.substring(0, 8) : "undefined";
        String date = LocalDate.now(clock).format(FILE_DATE);
        return String.format("reporte_%s_%s_%s.xlsx", safeName, shortId, date).toLowerCase();
    }
}
