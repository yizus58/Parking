package com.nelumbo.park.infrastructure.adapter.out.report;

import com.nelumbo.park.domain.model.report.VehicleDetail;
import com.nelumbo.park.domain.model.report.VehicleOutDetail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PdfAmountFormatTest {

    private final Locale original = Locale.getDefault();

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(original);
    }

    @ParameterizedTest
    @ValueSource(strings = {"en-US", "es-CO", "de-DE", "fr-FR"})
    void amountsAreLocaleIndependent(String tag) {
        Locale.setDefault(Locale.forLanguageTag(tag));

        assertEquals("$1,234,567.50", Pdf.formatAmount(1234567.5f));
        assertEquals("$0.00", Pdf.formatAmount(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fr-FR", "es-CO"})
    void pdfRendersUnderLocalesWhoseGroupingCharIsNotInTheStandardFonts(String tag) {
        Locale.setDefault(Locale.forLanguageTag(tag));
        VehicleOutDetail report = new VehicleOutDetail("u1", "socio", "s@example.com", "p1", "Centro",
                List.of(new VehicleDetail("v1", "ABC123", "Mazda", "20-11-2026-10:00", "20-11-2026-12:00", 1234567.5f)),
                1, 1234567.5f);

        assertDoesNotThrow(() -> new Pdf().generarPdfPorUsuario(List.of(report)));
    }
}
