package com.nelumbo.park.domain.model.stats;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

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

    /**
     * Day 1 at 05:00 to day 1 of the next month at 23:59:59.999. The end
     * deliberately keeps the original behaviour (one day past the month).
     */
    public static RankingPeriod monthOf(LocalDateTime now) {
        LocalDateTime start = now.withDayOfMonth(1)
                .withHour(START_HOUR).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusMonths(1)
                .withHour(23).withMinute(59).withSecond(59).withNano(END_OF_DAY_NANOS);
        return new RankingPeriod(start, end);
    }
}
