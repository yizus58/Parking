package com.nelumbo.park.service.infrastructure;

import com.nelumbo.park.dto.response.VehicleOutDetailResponse;
import com.nelumbo.park.service.VehicleReportService;
import com.nelumbo.park.utils.Excel;
import com.nelumbo.park.utils.ExcelComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CronServiceTest {

    @Mock
    private VehicleReportService vehicleReportService;
    @Mock
    private Excel excel;
    @Mock
    private ExcelComponent excelGenerator;
    @Mock
    private S3Service s3Service;
    @Mock
    private RabbitMQService rabbitMQService;

    @InjectMocks
    private CronService cronService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cronService, "subject", "Test Subject");
        ReflectionTestUtils.setField(cronService, "typeMessage", "Test Type Message");
    }

    @Test
    @DisplayName("Should return false when no vehicle out details are found")
    void runDailyTask_NoVehicleOutDetails_ReturnsFalse() {
        when(vehicleReportService.getVehiclesOutDetails()).thenReturn(Collections.emptyList());

        boolean result = cronService.runDailyTask();

        assertFalse(result);
        verify(vehicleReportService, times(1)).getVehiclesOutDetails();
        verifyNoMoreInteractions(vehicleReportService);
        verifyNoInteractions(excel, excelGenerator, s3Service, rabbitMQService);
    }

    @Test
    @DisplayName("Should return true and process vehicles when details are found")
    void runDailyTask_WithVehicleOutDetails_ReturnsTrueAndProcesses() throws IOException {
        VehicleOutDetailResponse detail1 = new VehicleOutDetailResponse();
        detail1.setUserId("user1");
        detail1.setEmail("user1@example.com");
        detail1.setParking("parking1");

        VehicleOutDetailResponse detail2 = new VehicleOutDetailResponse();
        detail2.setUserId("user2");
        detail2.setEmail("user2@example.com");
        detail2.setParking("parking2");

        List<VehicleOutDetailResponse> details = List.of(detail1, detail2);

        when(vehicleReportService.getVehiclesOutDetails()).thenReturn(details);
        when(excel.generarExcelPorUsuario(anyList())).thenReturn("test_excel_bytes".getBytes());
        when(excelGenerator.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        when(s3Service.uploadFile(any(byte[].class), anyString(), anyString())).thenReturn(Collections.singletonMap("Key", "someKey"));
        doNothing().when(rabbitMQService).publishMessageBackoff(any());

        boolean result = cronService.runDailyTask();

        assertTrue(result);
        verify(vehicleReportService, times(1)).getVehiclesOutDetails();
        verify(excel, times(2)).generarExcelPorUsuario(anyList());
        verify(excelGenerator, times(2)).getContentType();
        verify(s3Service, times(2)).uploadFile(any(byte[].class), anyString(), anyString());
        verify(rabbitMQService, times(2)).publishMessageBackoff(any());
    }

    @Test
    @DisplayName("Should handle IOException during excel generation gracefully")
    void runDailyTask_ExcelGenerationThrowsIOException_LogsErrorAndContinues() throws IOException {
        VehicleOutDetailResponse detail1 = new VehicleOutDetailResponse();
        detail1.setUserId("user1");
        detail1.setEmail("user1@example.com");
        detail1.setParking("parking1");

        List<VehicleOutDetailResponse> details = List.of(detail1);

        when(vehicleReportService.getVehiclesOutDetails()).thenReturn(details);
        when(excel.generarExcelPorUsuario(anyList())).thenThrow(new IOException("Test IO Exception"));

        boolean result = cronService.runDailyTask();

        assertTrue(result);
        verify(vehicleReportService, times(1)).getVehiclesOutDetails();
        verify(excel, times(1)).generarExcelPorUsuario(anyList());
        verify(excelGenerator, times(1)).getContentType();
        verify(s3Service, never()).uploadFile(any(byte[].class), anyString(), anyString());
        verify(rabbitMQService, never()).publishMessageBackoff(any());
    }

    @Test
    @DisplayName("Should handle Exception during S3 upload gracefully")
    void runDailyTask_S3UploadThrowsException_LogsErrorAndContinues() throws IOException {
        VehicleOutDetailResponse detail1 = new VehicleOutDetailResponse();
        detail1.setUserId("user1");
        detail1.setEmail("user1@example.com");
        detail1.setParking("parking1");

        List<VehicleOutDetailResponse> details = List.of(detail1);

        when(vehicleReportService.getVehiclesOutDetails()).thenReturn(details);
        when(excel.generarExcelPorUsuario(anyList())).thenReturn("test_excel_bytes".getBytes());
        when(excelGenerator.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        when(s3Service.uploadFile(any(byte[].class), anyString(), anyString())).thenThrow(new RuntimeException("Test S3 Exception"));

        boolean result = cronService.runDailyTask();

        assertTrue(result);
        verify(vehicleReportService, times(1)).getVehiclesOutDetails();
        verify(excel, times(1)).generarExcelPorUsuario(anyList());
        verify(excelGenerator, times(1)).getContentType();
        verify(s3Service, times(1)).uploadFile(any(byte[].class), anyString(), anyString());
        verify(rabbitMQService, never()).publishMessageBackoff(any());
    }

    @Test
    @DisplayName("Should handle Exception during email notification gracefully")
    void runDailyTask_EmailNotificationThrowsException_LogsErrorAndContinues() throws IOException {
        VehicleOutDetailResponse detail1 = new VehicleOutDetailResponse();
        detail1.setUserId("user1");
        detail1.setEmail("user1@example.com");
        detail1.setParking("parking1");

        List<VehicleOutDetailResponse> details = List.of(detail1);

        when(vehicleReportService.getVehiclesOutDetails()).thenReturn(details);
        when(excel.generarExcelPorUsuario(anyList())).thenReturn("test_excel_bytes".getBytes());
        when(excelGenerator.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        when(s3Service.uploadFile(any(byte[].class), anyString(), anyString())).thenReturn(Collections.singletonMap("Key", "someKey"));
        doThrow(new RuntimeException("Test RabbitMQ Exception")).when(rabbitMQService).publishMessageBackoff(any());

        boolean result = cronService.runDailyTask();

        assertTrue(result);
        verify(vehicleReportService, times(1)).getVehiclesOutDetails();
        verify(excel, times(1)).generarExcelPorUsuario(anyList());
        verify(excelGenerator, times(1)).getContentType();
        verify(s3Service, times(1)).uploadFile(any(byte[].class), anyString(), anyString());
        verify(rabbitMQService, times(1)).publishMessageBackoff(any());
    }
}
