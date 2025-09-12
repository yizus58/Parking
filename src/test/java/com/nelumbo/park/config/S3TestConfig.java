package com.nelumbo.park.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3TestConfig {
    @Bean
    public S3Client s3Client() {
        return Mockito.mock(S3Client.class);
    }
}
