package com.nelumbo.park.service.scheduler;

import com.nelumbo.park.service.CronService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CronScheduler {

    private CronService cronService;

    private static final Logger logger = LoggerFactory.getLogger(CronScheduler.class);

    @Scheduled(cron = "${cron.determination}", zone = "America/Bogota")
    public void executeDailyTask() {
        if (!cronService.runDailyTask()) {
            logger.error("Error en la ejecucion de la tarea");
        }
        logger.info("Ejecucion de la tarea exitosa");
    }
}
