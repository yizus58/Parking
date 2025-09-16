package com.nelumbo.park.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import java.net.URI;

@Configuration
public class CloudFlareConfig {

    @Value("${r2.bucket.access.key}")
    private String bucketAccessKey;

    @Value("${r2.bucket.secret.key}")
    private String bucketSecretKey;

    @Value("${r2.bucket.region}")
    private String bucketRegion;

    @Value("${r2.account.id}")
    private String accountId;

    @Bean
    public S3Client cloudFlare() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(bucketAccessKey, bucketSecretKey);
        return S3Client.builder()
                .region(Region.of(bucketRegion))
                .endpointOverride(URI.create("https://" + accountId + ".r2.cloudflarestorage.com"))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .forcePathStyle(true)
                .build();
    }
}
