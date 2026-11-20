package com.nelumbo.park.infrastructure.adapter.in.web.mapper;

import com.nelumbo.park.domain.model.stats.TopParking;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebMapperLocaleTest {

    private final Locale original = Locale.getDefault();

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(original);
    }

    @ParameterizedTest
    @ValueSource(strings = {"en-US", "es-CO", "de-DE", "fr-FR"})
    void topParkingTotalCost_usesADotWhateverTheDefaultLocale(String tag) {
        Locale.setDefault(Locale.forLanguageTag(tag));

        assertEquals("7500.0f", WebMapper.toTopParking(new TopParking("p1", "Centro", 3L, 7500f)).getTotalCost());
        assertEquals("1234.5f", WebMapper.toTopParking(new TopParking("p1", "Centro", 3L, 1234.5f)).getTotalCost());
    }

    @Test
    void topParkingKeepsTheOtherFields() {
        var response = WebMapper.toTopParking(new TopParking("p1", "Centro", 3L, 0f));

        assertEquals("p1", response.getParkingId());
        assertEquals("Centro", response.getParkingName());
        assertEquals(3L, response.getTotalVehicles());
        assertEquals("0.0f", response.getTotalCost());
    }
}
