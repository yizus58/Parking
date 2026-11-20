package com.nelumbo.park.infrastructure.adapter.out.report;

import com.nelumbo.park.domain.model.report.VehicleDetail;
import com.nelumbo.park.domain.model.report.VehicleOutDetail;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportRendererAdapterTest {

    private final VehicleOutDetail report = new VehicleOutDetail("u1", "socio", "s@example.com", "p1", "Centro",
            List.of(new VehicleDetail("v1", "ABC123", "Mazda", "20-11-2026-10:00", "20-11-2026-12:00", 4000f)),
            1, 4000f);

    @Test
    void rendersFilesAndHtml() throws Exception {
        ExcelComponent excelComponent = mock(ExcelComponent.class);
        ReportRendererAdapter adapter = new ReportRendererAdapter(new Excel(), excelComponent, new Pdf(), "application/pdf");

        assertTrue(adapter.renderExcel(report).length > 0);
        assertTrue(adapter.renderPdf(report).length > 0);
        assertTrue(adapter.renderEmailHtml(report).contains("<b>Centro</b>"));
        assertEquals("application/pdf", adapter.pdfContentType());
    }

    @Test
    void excelContentTypeFallsBackToDefault() {
        ExcelComponent excelComponent = mock(ExcelComponent.class);
        ReportRendererAdapter adapter = new ReportRendererAdapter(new Excel(), excelComponent, new Pdf(), "application/pdf");

        when(excelComponent.getContentType()).thenReturn("");
        assertEquals(ReportRendererAdapter.DEFAULT_XLSX_CONTENT_TYPE, adapter.excelContentType());
        when(excelComponent.getContentType()).thenReturn("custom/xlsx");
        assertEquals("custom/xlsx", adapter.excelContentType());
    }
}
