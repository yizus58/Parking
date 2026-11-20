package com.nelumbo.park.infrastructure.adapter.in.scheduler;

import com.nelumbo.park.application.port.in.SendDailyReportsUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class CronScheduler {

    private final SendDailyReportsUseCase sendDailyReports;

    private static final Logger logger = LoggerFactory.getLogger(CronScheduler.class);

    public CronScheduler(SendDailyReportsUseCase sendDailyReports) {
        this.sendDailyReports = sendDailyReports;
    }

    @Scheduled(cron = "${cron.determination}", zone = "America/Bogota")
    public void executeDailyTask() {
        if (!sendDailyReports.sendDailyReports()) {
            logger.error("Error en la ejecucion de la tarea");
            return;
        }
        logger.info("Ejecucion de la tarea exitosa");
    }
}