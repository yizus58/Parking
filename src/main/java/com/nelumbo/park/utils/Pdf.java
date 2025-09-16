package com.nelumbo.park.utils;

import com.nelumbo.park.dto.response.VehicleDetailResponse;
import com.nelumbo.park.dto.response.VehicleOutDetailResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component("pdfReport")
public class Pdf {

    public byte[] generarPdfPorUsuario(List<VehicleOutDetailResponse> data) throws IOException {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("generarPdfPorUsuario: 'data' vacío o inválido");
        }

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (VehicleOutDetailResponse item : data) {
                createUserPage(document, item);
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    private void createUserPage(PDDocument document, VehicleOutDetailResponse item) throws IOException {
        PDPage page = new PDPage();
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            // Add logo
            try {
                PDImageXObject logo = PDImageXObject.createFromFile("src/main/resources/images/nelumbo.png", document);
                contentStream.drawImage(logo, 50, 720, 100, 50); // Adjust position and size as needed
            } catch (IOException e) {
                // Handle logo not found, maybe log a warning
            }

            String username = Optional.ofNullable(item.getUsername()).orElse("usuario");
            String parkingName = Optional.ofNullable(item.getParking()).orElse("").toLowerCase();

            // Title
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
            contentStream.newLineAtOffset(160, 750); // Adjust position to be next to the logo
            contentStream.showText("Usuario: " + username + "  |  Parqueadero a cargo: " + parkingName);
            contentStream.endText();

            // Text
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
            float yPosition = 705; // Below the logo
            String text1 = "En la última hora se ha registrado un total de " + item.getVehicles().size() +
                    " vehículos en parqueadero " + parkingName + "." + "El total de dinero generado por el parqueadero es de " + item.getTotalEarnings();

            String text2 = "A continuación encontrarás con más detalle el registro";

            contentStream.beginText();
            contentStream.newLineAtOffset(50, yPosition);
            contentStream.showText(text1);
            contentStream.endText();

            yPosition -= 15; // Move down for the next line

            contentStream.beginText();
            contentStream.newLineAtOffset(50, yPosition);
            contentStream.showText(text2);
            contentStream.endText();

            // Table
            drawTable(contentStream, item);

            // Footer
            drawFooter(contentStream);
        }
    }

    private void drawTable(PDPageContentStream contentStream, VehicleOutDetailResponse item) throws IOException {
        final float margin = 50;
        final float yStart = 670; // Adjusted yStart to make space for the new text
        final float tableWidth = 500;
        final float rowHeight = 20;
        final float tableTopY = yStart - rowHeight;

        // Headers
        String[] headers = {"#", "Placa", "Modelo", "Fecha Entrada", "Fecha Salida", "Dinero Generado"};
        float[] colWidths = {30, 80, 110, 90, 90, 100};

        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);
        float nextX = margin;
        float y = yStart;

        for (int i = 0; i < headers.length; i++) {
            contentStream.beginText();
            contentStream.newLineAtOffset(nextX + 5, y - 15); // Add some padding
            contentStream.showText(headers[i]);
            contentStream.endText();
            nextX += colWidths[i];
        }

        // Draw header separator line
        contentStream.setStrokingColor(Color.BLACK);
        contentStream.moveTo(margin, y - rowHeight);
        contentStream.lineTo(margin + tableWidth, y - rowHeight);
        contentStream.stroke();

        // Table data
        List<VehicleDetailResponse> vehicles = item.getVehicles();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        y = tableTopY;

        if (vehicles != null && !vehicles.isEmpty()) {
            int rowNum = 1;
            for (VehicleDetailResponse v : vehicles) {
                y -= rowHeight;
                nextX = margin;

                String[] rowData = {
                        String.valueOf(rowNum++),
                        Optional.ofNullable(v.getPlateNumber()).orElse(""),
                        Optional.ofNullable(v.getModelVehicle()).orElse(""),
                        Optional.ofNullable(v.getDayEntry()).orElse(""),
                        Optional.ofNullable(v.getDayExit()).orElse(""),
                        String.format("$%,.2f", v.getTotalCost())
                };

                for (int i = 0; i < rowData.length; i++) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(nextX + 5, y); // Add some padding
                    contentStream.showText(rowData[i]);
                    contentStream.endText();
                    nextX += colWidths[i];
                }
            }
            // Draw subtotal
            y -= rowHeight * 1.5; // A bit of space
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10);

            float subtotalLabelX = margin + colWidths[0] + colWidths[1] + colWidths[2] + colWidths[3];
            contentStream.beginText();
            contentStream.newLineAtOffset(subtotalLabelX, y);
            contentStream.showText("Subtotal:");
            contentStream.endText();

            float subtotalValueX = subtotalLabelX + colWidths[4];
            contentStream.beginText();
            contentStream.newLineAtOffset(subtotalValueX, y);
            contentStream.showText(String.format("$%,.2f", item.getTotalEarnings()));
            contentStream.endText();

        } else {
            y -= rowHeight;
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, y);
            contentStream.showText("Sin registros de vehículos");
            contentStream.endText();
        }
    }

    private void drawFooter(PDPageContentStream contentStream) throws IOException {
        float footerY = 30;
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 8);
        contentStream.beginText();
        contentStream.newLineAtOffset(50, footerY);
        contentStream.showText("Reporte generado por Nelumbo Park.");
        contentStream.endText();
    }
}
