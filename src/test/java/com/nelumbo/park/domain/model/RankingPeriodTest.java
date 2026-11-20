package com.nelumbo.park.domain.model;

import com.nelumbo.park.domain.model.stats.RankingPeriod;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RankingPeriodTest {

    @Test
    void weekOf_startsMonday0500_endsSunday235959() {
        RankingPeriod week = RankingPeriod.weekOf(LocalDateTime.of(2026, 11, 20, 14, 42));

        assertEquals(LocalDateTime.of(2026, 11, 16, 5, 0), week.start());
        assertEquals(LocalDateTime.of(2026, 11, 22, 23, 59, 59, 999_000_000), week.end());
    }

    @Test
    void monthOf_startsDayOne0500() {
        RankingPeriod month = RankingPeriod.monthOf(LocalDateTime.of(2026, 11, 20, 14, 42));

        assertEquals(LocalDateTime.of(2026, 11, 1, 5, 0), month.start());
        assertEquals(LocalDateTime.of(2026, 11, 30, 23, 59, 59, 999_000_000), month.end());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "2026-01-15T10:00, 2026-01-31", "2026-02-01T00:00, 2026-02-28", "2028-02-29T23:00, 2028-02-29",
            "2026-04-30T23:59, 2026-04-30", "2026-12-31T12:00, 2026-12-31"})
    void monthOf_endsOnTheLastDayOfThatMonth(LocalDateTime now, java.time.LocalDate lastDay) {
        RankingPeriod month = RankingPeriod.monthOf(now);

        assertEquals(lastDay.atTime(23, 59, 59, 999_000_000), month.end());
        assertEquals(now.getMonth(), month.end().getMonth());
        assertEquals(1, month.start().getDayOfMonth());
    }
}
