package com.nelumbo.park.domain.model.stats;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Time window used by the rankings, in local (server) time.
 */
public record RankingPeriod(LocalDateTime start, LocalDateTime end) {

    private static final int START_HOUR = 5;
    private static final int END_OF_DAY_NANOS = 999_000_000;

    /** Monday 05:00 to Sunday 23:59:59.999 of the week containing {@code now}. */
    public static RankingPeriod weekOf(LocalDateTime now) {
        LocalDateTime start = now.with(DayOfWeek.MONDAY)
                .withHour(START_HOUR).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusDays(6)
                .withHour(23).withMinute(59).withSecond(59).withNano(END_OF_DAY_NANOS);
        return new RankingPeriod(start, end);
    }

    /** Day 1 at 05:00 to the last day of the month at 23:59:59.999 (28 Feb, 29 Feb in leap years, 30, 31...). */
    public static RankingPeriod monthOf(LocalDateTime now) {
        LocalDateTime start = now.withDayOfMonth(1)
                .withHour(START_HOUR).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = now.with(TemporalAdjusters.lastDayOfMonth())
                .withHour(23).withMinute(59).withSecond(59).withNano(END_OF_DAY_NANOS);
        return new RankingPeriod(start, end);
    }
}
