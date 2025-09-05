package com.nelumbo.park.utils;

import com.nelumbo.park.dto.response.VehicleDetailResponse;
import com.nelumbo.park.dto.response.VehicleOutDetailResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
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

@Component("/views/report.xlsx")
public class Excel {

    public byte[] generarExcelPorUsuario(List<VehicleOutDetailResponse> data) throws IOException {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("generarExcelPorUsuario: 'data' vacío o inválido");
        }
        XSSFWorkbook workbook = new XSSFWorkbook();
        CreationHelper createHelper = workbook.getCreationHelper();

        for (VehicleOutDetailResponse item : data) {
            String sheetName = Optional.ofNullable(item.getUsername()).orElse("usuario");
            sheetName = sheetName.length() > 31 ? sheetName.substring(0, 31) : sheetName;

            XSSFSheet sheet = workbook.createSheet(sheetName);
            String nameParking = Optional.ofNullable(item.getParking()).orElse("").toLowerCase();

            // Estilo de encabezado
            CellStyle boldStyle = workbook.createCellStyle();
            XSSFFont boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldFont.setFontHeightInPoints((short) 12);
            boldStyle.setFont(boldFont);

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

            int rowIndex = 3;
            List<VehicleDetailResponse> vehicles = item.getVehicles();

            if (vehicles != null && !vehicles.isEmpty()) {
                for (int i = 0; i < vehicles.size(); i++) {
                    VehicleDetailResponse v = vehicles.get(i);
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(i + 1);
                    row.createCell(1).setCellValue(Optional.ofNullable(v.getPlateNumber()).orElse(""));
                    row.createCell(2).setCellValue(Optional.ofNullable(v.getModelVehicle()).orElse(""));
                    row.createCell(3).setCellValue(Optional.ofNullable(v.getDay()).orElse(""));
                    row.createCell(4).setCellValue(v.getTotalCost());
                }

                // Subtotal
                Row subtotalRow = sheet.createRow(rowIndex++);
                subtotalRow.createCell(3).setCellValue("Subtotal:");
                Cell subtotalCell = subtotalRow.createCell(4);
                if (item.getTotalEarnings() != null) {
                    subtotalCell.setCellValue(item.getTotalEarnings());
                } else {
                    String formula = String.format("SUM(E4:E%d)", rowIndex - 2);
                    subtotalCell.setCellFormula(formula);
                }
                subtotalCell.setCellStyle(boldStyle);
            } else {
                sheet.createRow(rowIndex++);
                sheet.createRow(rowIndex).createCell(0).setCellValue("Sin registros de vehículos");
            }

            // Ancho de columnas
            int[] colWidths = {5, 15, 25, 15, 15};
            for (int i = 0; i < colWidths.length; i++) {
                sheet.setColumnWidth(i, colWidths[i] * 256);
            }

            // Filtro
            sheet.setAutoFilter(new CellRangeAddress(2, 2, 0, 4));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }


}
