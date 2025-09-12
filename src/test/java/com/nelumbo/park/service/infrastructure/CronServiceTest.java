package com.nelumbo.park.service.infrastructure;

import com.nelumbo.park.dto.response.EmailDataResponse;
import com.nelumbo.park.dto.response.RabbitMQResponse;
import com.nelumbo.park.dto.response.VehicleOutDetailResponse;
import com.nelumbo.park.service.VehicleReportService;
import com.nelumbo.park.utils.Excel;
import com.nelumbo.park.utils.ExcelComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
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
    @DisplayName("Should return false when vehicle out details are null")
    void runDailyTask_NullVehicleOutDetails_ReturnsFalse() {
        when(vehicleReportService.getVehiclesOutDetails()).thenReturn(null);

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
        detail1.setUsername("User One");
        detail1.setTotalVehicles(10);

        VehicleOutDetailResponse detail2 = new VehicleOutDetailResponse();
        detail2.setUserId("user2");
        detail2.setEmail("user2@example.com");
        detail2.setParking("parking2");
        detail2.setUsername("User Two");
        detail2.setTotalVehicles(5);

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
    @DisplayName("Should generate correct file names for various vehicle details")
    void runDailyTask_GenerateFileNames_Correctly() throws IOException {
        VehicleOutDetailResponse detail1 = new VehicleOutDetailResponse();
        detail1.setUserId("user123456789");
        detail1.setEmail("user1@example.com");
        detail1.setParking("parking_with_a_very_long_name_that_should_be_truncated_to_fifty_characters_and_more");
        detail1.setUsername("Test User");
        detail1.setTotalVehicles(15);

        List<VehicleOutDetailResponse> details = List.of(detail1);

        when(vehicleReportService.getVehiclesOutDetails()).thenReturn(details);
        when(excel.generarExcelPorUsuario(anyList())).thenReturn("test_excel_bytes".getBytes());
        when(excelGenerator.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        when(s3Service.uploadFile(any(byte[].class), anyString(), anyString())).thenReturn(Collections.singletonMap("Key", "someKey"));
        doNothing().when(rabbitMQService).publishMessageBackoff(any());

        boolean result = cronService.runDailyTask();

        assertTrue(result);
        verify(vehicleReportService, times(1)).getVehiclesOutDetails();
        verify(excel, times(1)).generarExcelPorUsuario(anyList());
        verify(excelGenerator, times(1)).getContentType();

        verify(s3Service, times(1)).uploadFile(any(byte[].class), anyString(), anyString());

        ArgumentCaptor<Object> rabbitMQMessageCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitMQService, times(1)).publishMessageBackoff(rabbitMQMessageCaptor.capture());

        RabbitMQResponse capturedResponse = (RabbitMQResponse) rabbitMQMessageCaptor.getValue();
        List<EmailDataResponse> emailDataList = (List<EmailDataResponse>) capturedResponse.getData();
        assertFalse(emailDataList.isEmpty());
        EmailDataResponse emailData = emailDataList.get(0);
        String fileName1 = emailData.getAttachments().get(0).getNameFile();

        String expectedFixedPrefix = "reporte_parking_with_a_very_long_name_that_should_be_trunc_user1234_";
        assertTrue(fileName1.startsWith(expectedFixedPrefix), "Filename should start with the expected prefix.");

        String datePartAndSuffix = fileName1.substring(expectedFixedPrefix.length());
        assertTrue(datePartAndSuffix.matches("\\d{8}\\.xlsx"), "Filename should contain a date and end with .xlsx.");
    }

    @Test
    @DisplayName("Should use default content type when excelGenerator returns null or empty")
    void runDailyTask_ContentTypeNullOrEmpty_UsesDefault() throws IOException {
        VehicleOutDetailResponse detail1 = new VehicleOutDetailResponse();
        detail1.setUserId("user1");
        detail1.setEmail("user1@example.com");
        detail1.setParking("parking1");
        detail1.setUsername("User One");
        detail1.setTotalVehicles(10);

        List<VehicleOutDetailResponse> details = List.of(detail1);

        when(vehicleReportService.getVehiclesOutDetails()).thenReturn(details);
        when(excel.generarExcelPorUsuario(anyList())).thenReturn("test_excel_bytes".getBytes());

        when(excelGenerator.getContentType())
                .thenReturn(null)
                .thenReturn("");

        when(s3Service.uploadFile(any(byte[].class), anyString(), anyString())).thenReturn(Collections.singletonMap("Key", "someKey"));
        doNothing().when(rabbitMQService).publishMessageBackoff(any());

        cronService.runDailyTask();
        cronService.runDailyTask();

        verify(vehicleReportService, times(2)).getVehiclesOutDetails();
        verify(excel, times(2)).generarExcelPorUsuario(anyList());
        verify(excelGenerator, times(2)).getContentType();

        ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor.forClass(String.class);
        verify(s3Service, times(2)).uploadFile(any(byte[].class), contentTypeCaptor.capture(), anyString());

        List<String> capturedContentTypes = contentTypeCaptor.getAllValues();
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", capturedContentTypes.get(0));
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", capturedContentTypes.get(1));

        verify(rabbitMQService, times(2)).publishMessageBackoff(any());
    }

    @Test
    @DisplayName("Should handle IOException during excel generation gracefully")
    void runDailyTask_ExcelGenerationThrowsIOException_LogsErrorAndContinues() throws IOException {
        VehicleOutDetailResponse detail1 = new VehicleOutDetailResponse();
        detail1.setUserId("user1");
        detail1.setEmail("user1@example.com");
        detail1.setParking("parking1");
        detail1.setUsername("User One");
        detail1.setTotalVehicles(10);

        List<VehicleOutDetailResponse> details = List.of(detail1);

        when(vehicleReportService.getVehiclesOutDetails()).thenReturn(details);
        when(excel.generarExcelPorUsuario(anyList())).thenThrow(new IOException("Test IO Exception"));
        when(excelGenerator.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");


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
        detail1.setUsername("User One");
        detail1.setTotalVehicles(10);

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
    @DisplayName("Should not send email if S3 upload returns null")
    void runDailyTask_S3UploadReturnsNull_NoEmailSent() throws IOException {
        VehicleOutDetailResponse detail1 = new VehicleOutDetailResponse();
        detail1.setUserId("user1");
        detail1.setEmail("user1@example.com");
        detail1.setParking("parking1");
        detail1.setUsername("User One");
        detail1.setTotalVehicles(10);

        List<VehicleOutDetailResponse> details = List.of(detail1);

        when(vehicleReportService.getVehiclesOutDetails()).thenReturn(details);
        when(excel.generarExcelPorUsuario(anyList())).thenReturn("test_excel_bytes".getBytes());
        when(excelGenerator.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        when(s3Service.uploadFile(any(byte[].class), anyString(), anyString())).thenReturn(null);

        boolean result = cronService.runDailyTask();

        assertTrue(result);
        verify(vehicleReportService, times(1)).getVehiclesOutDetails();
        verify(excel, times(1)).generarExcelPorUsuario(anyList());
        verify(excelGenerator, times(1)).getContentType();
        verify(s3Service, times(1)).uploadFile(any(byte[].class), anyString(), anyString());
        verifyNoInteractions(rabbitMQService);
    }

    @Test
    @DisplayName("Should not send email if S3 upload returns map without 'Key'")
    void runDailyTask_S3UploadReturnsMapWithoutKey_NoEmailSent() throws IOException {
        VehicleOutDetailResponse detail1 = new VehicleOutDetailResponse();
        detail1.setUserId("user1");
        detail1.setEmail("user1@example.com");
        detail1.setParking("parking1");
        detail1.setUsername("User One");
        detail1.setTotalVehicles(10);

        List<VehicleOutDetailResponse> details = List.of(detail1);

        when(vehicleReportService.getVehiclesOutDetails()).thenReturn(details);
        when(excel.generarExcelPorUsuario(anyList())).thenReturn("test_excel_bytes".getBytes());
        when(excelGenerator.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        when(s3Service.uploadFile(any(byte[].class), anyString(), anyString())).thenReturn(Collections.singletonMap("OtherKey", "someKey"));

        boolean result = cronService.runDailyTask();

        assertTrue(result);
        verify(vehicleReportService, times(1)).getVehiclesOutDetails();
        verify(excel, times(1)).generarExcelPorUsuario(anyList());
        verify(excelGenerator, times(1)).getContentType();
        verify(s3Service, times(1)).uploadFile(any(byte[].class), anyString(), anyString());
        verifyNoInteractions(rabbitMQService);
    }

    @Test
    @DisplayName("Should handle Exception during email notification gracefully")
    void runDailyTask_EmailNotificationThrowsException_LogsErrorAndContinues() throws IOException {
        VehicleOutDetailResponse detail1 = new VehicleOutDetailResponse();
        detail1.setUserId("user1");
        detail1.setEmail("user1@example.com");
        detail1.setParking("parking1");
        detail1.setUsername("User One");
        detail1.setTotalVehicles(10);

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

    @Test
    @DisplayName("Should send email notification with empty email if vehicle email is null")
    void runDailyTask_VehicleEmailIsNull_SendsEmailWithEmptyString() throws IOException {
        VehicleOutDetailResponse detail1 = new VehicleOutDetailResponse();
        detail1.setUserId("user1");
        detail1.setEmail(null);
        detail1.setParking("parking1");
        detail1.setUsername("User One");
        detail1.setTotalVehicles(10);

        List<VehicleOutDetailResponse> details = List.of(detail1);

        when(vehicleReportService.getVehiclesOutDetails()).thenReturn(details);
        when(excel.generarExcelPorUsuario(anyList())).thenReturn("test_excel_bytes".getBytes());
        when(excelGenerator.getContentType()).thenReturn("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        when(s3Service.uploadFile(any(byte[].class), anyString(), anyString())).thenReturn(Collections.singletonMap("Key", "someKey"));
        doNothing().when(rabbitMQService).publishMessageBackoff(any());

        boolean result = cronService.runDailyTask();

        assertTrue(result);
        verify(vehicleReportService, times(1)).getVehiclesOutDetails();
        verify(excel, times(1)).generarExcelPorUsuario(anyList());
        verify(excelGenerator, times(1)).getContentType();
        verify(s3Service, times(1)).uploadFile(any(byte[].class), anyString(), anyString());

        ArgumentCaptor<Object> rabbitMQMessageCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitMQService, times(1)).publishMessageBackoff(rabbitMQMessageCaptor.capture());

        RabbitMQResponse capturedResponse = (RabbitMQResponse) rabbitMQMessageCaptor.getValue();
        List<EmailDataResponse> emailDataList = (List<EmailDataResponse>) capturedResponse.getData();
        assertFalse(emailDataList.isEmpty());
        EmailDataResponse emailData = emailDataList.get(0);
        assertEquals("", emailData.getRecipients().get(0));
    }

    @Test
    @DisplayName("Should handle general Exception during processVehicleReport gracefully")
    void runDailyTask_GeneralExceptionInProcessVehicleReport_LogsErrorAndContinues() throws IOException {
        VehicleOutDetailResponse detail1 = new VehicleOutDetailResponse();
        detail1.setUserId("user1");
        detail1.setEmail("user1@example.com");
        detail1.setParking("parking1");
        detail1.setUsername("User One");
        detail1.setTotalVehicles(10);

        List<VehicleOutDetailResponse> details = List.of(detail1);

        when(vehicleReportService.getVehiclesOutDetails()).thenReturn(details);

        when(excelGenerator.getContentType()).thenThrow(new RuntimeException("Simulated general exception"));

        boolean result = cronService.runDailyTask();

        assertTrue(result);
        verify(vehicleReportService, times(1)).getVehiclesOutDetails();
        verify(excelGenerator, times(1)).getContentType();
        verify(excel, never()).generarExcelPorUsuario(anyList());
        verify(s3Service, never()).uploadFile(any(byte[].class), anyString(), anyString());
        verify(rabbitMQService, never()).publishMessageBackoff(any());
    }
}
