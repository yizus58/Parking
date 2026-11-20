package com.nelumbo.park.infrastructure.adapter.out.report;

import com.nelumbo.park.application.port.out.ReportRendererPort;
import com.nelumbo.park.domain.model.report.VehicleOutDetail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class ReportRendererAdapter implements ReportRendererPort {

    static final String DEFAULT_XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final Excel excel;
    private final ExcelComponent excelComponent;
    private final Pdf pdf;
    private final String pdfContentType;

    public ReportRendererAdapter(Excel excel, ExcelComponent excelComponent, Pdf pdf,
                                 @Value("${spring.mvc.contentnegotiation.media-types.pdf}") String pdfContentType) {
        this.excel = excel;
        this.excelComponent = excelComponent;
        this.pdf = pdf;
        this.pdfContentType = pdfContentType;
    }

    @Override
    public byte[] renderExcel(VehicleOutDetail report) throws IOException {
        return excel.generarExcelPorUsuario(List.of(report));
    }

    @Override
    public String excelContentType() {
        String contentType = excelComponent.getContentType();
        return contentType == null || contentType.isEmpty() ? DEFAULT_XLSX_CONTENT_TYPE : contentType;
    }

    @Override
    public byte[] renderPdf(VehicleOutDetail report) throws IOException {
        return pdf.generarPdfPorUsuario(List.of(report));
    }

    @Override
    public String pdfContentType() {
        return pdfContentType;
    }

    @Override
    public String renderEmailHtml(VehicleOutDetail report) {
        return HtmlGenerator.generateHtmlContent(report);
    }
}
