package com.nelumbo.park.infrastructure.adapter.in.scheduler;

import com.nelumbo.park.application.port.in.SendDailyReportsUseCase;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class CronSchedulerTest {

    @Test
    void delegatesToUseCase() {
        SendDailyReportsUseCase useCase = mock(SendDailyReportsUseCase.class);
        when(useCase.sendDailyReports()).thenReturn(true).thenReturn(false);
        CronScheduler scheduler = new CronScheduler(useCase);

        scheduler.executeDailyTask();
        scheduler.executeDailyTask();

        verify(useCase, times(2)).sendDailyReports();
    }
}
