package com.nelumbo.park.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** Server default zone, the same the code used implicitly before. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
