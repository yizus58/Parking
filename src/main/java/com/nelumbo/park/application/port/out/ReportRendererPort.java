package com.nelumbo.park.application.port.out;

import com.nelumbo.park.domain.model.report.VehicleOutDetail;

import java.io.IOException;

/**
 * Renders the daily report of one partner and parking.
 */
public interface ReportRendererPort {

    byte[] renderExcel(VehicleOutDetail report) throws IOException;

    String excelContentType();

    byte[] renderPdf(VehicleOutDetail report) throws IOException;

    String pdfContentType();

    String renderEmailHtml(VehicleOutDetail report);
}
