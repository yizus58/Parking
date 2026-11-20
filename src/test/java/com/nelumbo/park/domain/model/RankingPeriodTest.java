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
        assertEquals(LocalDateTime.of(2026, 12, 1, 23, 59, 59, 999_000_000), month.end());
    }
}
