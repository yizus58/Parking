package com.nelumbo.park.utils;

import com.nelumbo.park.dto.response.VehicleDetailResponse;
import com.nelumbo.park.dto.response.VehicleOutDetailResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component("report")
public class Excel {

    public byte[] generarExcelPorUsuario(List<VehicleOutDetailResponse> data) throws IOException {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("generarExcelPorUsuario: 'data' vacío o inválido");
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (VehicleOutDetailResponse item : data) {
                createUserSheet(workbook, item);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createUserSheet(XSSFWorkbook workbook, VehicleOutDetailResponse item) {
        String sheetName = Optional.ofNullable(item.getUsername()).orElse("usuario");
        sheetName = sheetName.length() > 31 ? sheetName.substring(0, 31) : sheetName;

        XSSFSheet sheet = workbook.createSheet(sheetName);
        CellStyle boldStyle = createBoldStyle(workbook);

        createSheetHeaders(sheet, item, boldStyle);
        processVehicleData(sheet, item, boldStyle);
        configureColumnWidths(sheet);
        sheet.setAutoFilter(new CellRangeAddress(2, 2, 0, 4));
    }

    private CellStyle createBoldStyle(XSSFWorkbook workbook) {
        CellStyle boldStyle = workbook.createCellStyle();
        XSSFFont boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldFont.setFontHeightInPoints((short) 12);
        boldStyle.setFont(boldFont);
        return boldStyle;
    }

    private void createSheetHeaders(XSSFSheet sheet, VehicleOutDetailResponse item, CellStyle boldStyle) {
        String nameParking = Optional.ofNullable(item.getParking()).orElse("").toLowerCase();

        // Fila título
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Usuario: " + item.getUsername() + "  |  Parqueadero a cargo: " + nameParking);
        titleCell.setCellStyle(boldStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

        // Encabezados
        sheet.createRow(1);
        Row headerRow = sheet.createRow(2);
        String[] headers = {"#", "Placa", "Modelo", "Fecha", "Costo"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(boldStyle);
        }
    }

    private int processVehicleData(XSSFSheet sheet, VehicleOutDetailResponse item, CellStyle boldStyle) {
        int rowIndex = 3;
        List<VehicleDetailResponse> vehicles = item.getVehicles();

        if (vehicles != null && !vehicles.isEmpty()) {
            rowIndex = addVehicleRows(sheet, vehicles, rowIndex);
            addSubtotalRow(sheet, item, rowIndex, boldStyle);
        } else {
            addNoDataMessage(sheet, rowIndex);
        }

        return rowIndex;
    }

    private int addVehicleRows(XSSFSheet sheet, List<VehicleDetailResponse> vehicles, int startRowIndex) {
        int rowIndex = startRowIndex;
        CellStyle currencyStyle = sheet.getWorkbook().createCellStyle();
        currencyStyle.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("$#,##0.00"));
        for (int i = 0; i < vehicles.size(); i++) {
            VehicleDetailResponse v = vehicles.get(i);
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue((double) i + 1);
            row.createCell(1).setCellValue(Optional.ofNullable(v.getPlateNumber()).orElse(""));
            row.createCell(2).setCellValue(Optional.ofNullable(v.getModelVehicle()).orElse(""));
            row.createCell(3).setCellValue(Optional.ofNullable(v.getDay()).orElse(""));
            Cell costCell = row.createCell(4);
            costCell.setCellValue(v.getTotalCost());
            costCell.setCellStyle(currencyStyle);
        }
        return rowIndex;
    }

    private void addSubtotalRow(XSSFSheet sheet, VehicleOutDetailResponse item, int rowIndex, CellStyle boldStyle) {
        Row subtotalRow = sheet.createRow(rowIndex);
        Cell labelCell = subtotalRow.createCell(3);
        labelCell.setCellValue("Subtotal:");
        labelCell.setCellStyle(boldStyle);
        Cell subtotalCell = subtotalRow.createCell(4);

        CellStyle boldCurrencyStyle = sheet.getWorkbook().createCellStyle();
        boldCurrencyStyle.cloneStyleFrom(boldStyle);
        boldCurrencyStyle.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("$#,##0.00"));

        if (item.getTotalEarnings() != null) {
            subtotalCell.setCellValue(item.getTotalEarnings());
        } else {
            String formula = String.format("SUM(E4:E%d)", rowIndex);
            subtotalCell.setCellFormula(formula);
        }
        subtotalCell.setCellStyle(boldCurrencyStyle);
    }

    private void addNoDataMessage(XSSFSheet sheet, int rowIndex) {
        sheet.createRow(rowIndex);
        sheet.createRow((int) ((double) rowIndex + 1)).createCell(0).setCellValue("Sin registros de vehículos");
    }

    private void configureColumnWidths(XSSFSheet sheet) {
        int[] colWidths = {5, 15, 25, 15, 15};
        for (int i = 0; i < colWidths.length; i++) {
            sheet.setColumnWidth(i, (int) (colWidths[i] * 256.0));
        }
    }


}