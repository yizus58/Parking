package com.nelumbo.park.application.service;

import com.nelumbo.park.application.port.out.EmailNotificationPort;
import com.nelumbo.park.application.port.out.EmailNotificationPort.EmailAttachment;
import com.nelumbo.park.application.port.out.FileStoragePort;
import com.nelumbo.park.application.port.out.ReportRendererPort;
import com.nelumbo.park.application.port.out.VehicleRepositoryPort;
import com.nelumbo.park.domain.model.Parking;
import com.nelumbo.park.domain.model.User;
import com.nelumbo.park.domain.model.Vehicle;
import com.nelumbo.park.domain.model.VehicleStatus;
import com.nelumbo.park.domain.model.report.VehicleOutDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DailyReportServiceTest {

    private VehicleRepositoryPort vehicles;
    private ReportRendererPort renderer;
    private FileStoragePort storage;
    private EmailNotificationPort email;
    private DailyReportService service;

    private final User owner = new User("abcdefgh-1234", "socio", "h", "socio@example.com", "SOCIO");
    private final Parking parking = new Parking("park-1", "Parking Centro #1", "addr", 5, 1000f, owner);

    @BeforeEach
    void setUp() throws IOException {
        vehicles = mock(VehicleRepositoryPort.class);
        renderer = mock(ReportRendererPort.class);
        storage = mock(FileStoragePort.class);
        email = mock(EmailNotificationPort.class);
        service = new DailyReportService(vehicles, renderer, storage, email, Fixtures.CLOCK);
        when(renderer.renderExcel(any())).thenReturn(new byte[]{1});
        when(renderer.renderPdf(any())).thenReturn(new byte[]{2});
        when(renderer.excelContentType()).thenReturn("xlsx-type");
        when(renderer.pdfContentType()).thenReturn("pdf-type");
        when(renderer.renderEmailHtml(any())).thenReturn("<html/>");
    }

    private Vehicle out(String id, long hours) {
        return new Vehicle(id, "ABC" + id, "M", Fixtures.NOW.minusSeconds(hours * 3600), Fixtures.NOW, 1000f,
                VehicleStatus.OUT, parking, owner);
    }

    @Test
    void nothingToReport() {
        when(vehicles.findExitedBetween(any(), any())).thenReturn(List.of());
        assertFalse(service.sendDailyReports());
        verifyNoInteractions(storage, email);
    }

    @Test
    void queriesTodayInServerZone_andIgnoresVehiclesStillInside() {
        Vehicle stillIn = out("2", 1);
        stillIn.setStatus(VehicleStatus.IN);
        when(vehicles.findExitedBetween(Instant.parse("2026-11-20T05:00:00Z"), Instant.parse("2026-11-21T04:59:59.999Z")))
                .thenReturn(List.of(stillIn));

        assertFalse(service.sendDailyReports());
    }

    @Test
    void uploadsBothFilesAndEmailsAttachments() {
        when(vehicles.findExitedBetween(any(), any())).thenReturn(List.of(out("1", 2)));
        when(storage.store(any(), anyString(), anyString())).thenReturn(true);

        assertTrue(service.sendDailyReports());

        verify(storage).store(any(), eq("xlsx-type"), anyString());
        verify(storage).store(any(), eq("pdf-type"), anyString());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EmailAttachment>> captor = ArgumentCaptor.forClass(List.class);
        verify(email).sendEmail(eq("socio@example.com"), eq("<html/>"), captor.capture());
        List<EmailAttachment> attachments = captor.getValue();
        assertEquals("reporte_parking_centro_1_abcdefgh_20261120.xlsx", attachments.get(0).fileName());
        assertEquals("reporte_parking_centro_1_abcdefgh_20261120.pdf", attachments.get(1).fileName());
        assertEquals(32, attachments.get(0).storageKey().length());
    }

    @Test
    void failedUploadsSendNoEmail() {
        when(vehicles.findExitedBetween(any(), any())).thenReturn(List.of(out("1", 2)));
        when(storage.store(any(), anyString(), anyString())).thenThrow(new RuntimeException("down"));

        assertTrue(service.sendDailyReports());
        verifyNoInteractions(email);
    }

    @Test
    void renderingErrorIsLoggedAndOtherReportsContinue() throws IOException {
        User other = new User("zzzzzzzz-9", "otro", "h", "otro@example.com", "SOCIO");
        Parking otherParking = new Parking("park-2", "Otro", "addr", 5, 1000f, other);
        Vehicle second = new Vehicle("9", "XYZ9", "M", Fixtures.NOW.minusSeconds(3600), Fixtures.NOW, 1000f,
                VehicleStatus.OUT, otherParking, other);
        when(vehicles.findExitedBetween(any(), any())).thenReturn(List.of(out("1", 5), second));
        when(renderer.renderExcel(any())).thenThrow(new IOException("boom")).thenReturn(new byte[]{1});
        when(storage.store(any(), anyString(), anyString())).thenReturn(true);

        assertTrue(service.sendDailyReports());
        verify(email, times(1)).sendEmail(eq("otro@example.com"), any(), any());
    }

    @Test
    void emailFailureIsSwallowed_andNullEmailBecomesEmpty() {
        owner.setEmail(null);
        when(vehicles.findExitedBetween(any(), any())).thenReturn(List.of(out("1", 1)));
        when(storage.store(any(), anyString(), anyString())).thenReturn(true);
        doThrow(new RuntimeException("rabbit down")).when(email).sendEmail(any(), any(), any());

        assertTrue(service.sendDailyReports());
        verify(email).sendEmail(eq(""), any(), any());
    }

    @Test
    void fileName_fallbacks() {
        VehicleOutDetail report = new VehicleOutDetail(null, "u", "e", "p", null, List.of(), 0, 0f);
        assertEquals("reporte_diario_undefined_20261120.xlsx", service.fileName(report));
    }
}
