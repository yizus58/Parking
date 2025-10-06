package com.nelumbo.park.config.dotenv;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

            Map<String, Object> dotenvProperties = new HashMap<>();

            dotenv.entries().forEach(entry -> 
                dotenvProperties.put(entry.getKey(), entry.getValue())
            );

            if (!dotenvProperties.isEmpty()) {
                environment.getPropertySources().addLast(new MapPropertySource("dotenv", dotenvProperties));
            }

        } catch (Exception e) {
            logger.error("Error cargando .env: {} ", e.getMessage());
        }
    }
}
