package com.nelumbo.park;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        System.out.println("--- DEBUGGING ENVIRONMENT VARIABLES ---");
        System.out.println("SPRING_DATASOURCE_URL: " + System.getenv("SPRING_DATASOURCE_URL"));
        System.out.println("SPRING_DATASOURCE_USERNAME: " + System.getenv("SPRING_DATASOURCE_USERNAME"));
        System.out.println("SPRING_DATASOURCE_PASSWORD: " + System.getenv("SPRING_DATASOURCE_PASSWORD"));
        System.out.println("SPRING_DATASOURCE_DRIVER_CLASS_NAME: " + System.getenv("SPRING_DATASOURCE_DRIVER_CLASS_NAME"));
        System.out.println("------------------------------------");
        SpringApplication.run(Application.class, args);
    }
}
