package com.nelumbo.park.service.infrastructure.scheduler;

import com.nelumbo.park.service.infrastructure.CronService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CronScheduler {

    private final CronService cronService;

    private static final Logger logger = LoggerFactory.getLogger(CronScheduler.class);

    public CronScheduler(CronService cronService) {
        this.cronService = cronService;
    }

    @Scheduled(cron = "${cron.thirty.seconds}", zone = "America/Bogota")
    public void executeDailyTask() {
        if (!cronService.runDailyTask()) {
            logger.error("Error en la ejecucion de la tarea");
            return;
        }
        logger.info("Ejecucion de la tarea exitosa");
    }
}