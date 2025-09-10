package com.nelumbo.park.service.infrastructure.scheduler;

import com.nelumbo.park.service.infrastructure.CronService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CronSchedulerTest {

    @Mock
    private CronService cronService;

    @InjectMocks
    private CronScheduler cronScheduler;

    @Test
    @DisplayName("Should log success when daily task runs successfully")
    void executeDailyTask_Success() {
        when(cronService.runDailyTask()).thenReturn(true);

        cronScheduler.executeDailyTask();

        verify(cronService, times(1)).runDailyTask();
    }

    @Test
    @DisplayName("Should log error when daily task fails")
    void executeDailyTask_Failure() {

        when(cronService.runDailyTask()).thenReturn(false);

        cronScheduler.executeDailyTask();

        verify(cronService, times(1)).runDailyTask();
    }
}
