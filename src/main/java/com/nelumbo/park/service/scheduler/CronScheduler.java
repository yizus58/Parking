package com.nelumbo.park.service.scheduler;

import com.nelumbo.park.service.CronService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CronScheduler {

    @Autowired
    private CronService cronService;

    private static final Logger logger = LoggerFactory.getLogger(CronScheduler.class);

    @Scheduled(cron = "${cron.thirty.seconds}", zone = "America/Bogota")
    public void executeDailyTask() {
        if (!cronService.runDailyTask()) {
            System.out.println("La tarea no se pudo ejecutar correctamente");
        }
        System.out.println("La tarea se ejecuto correctamente");
    }
}
