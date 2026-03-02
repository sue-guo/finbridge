package io.github.sueguo.finbridge.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * AWS S3 client configuration.
 *
 * <p>Uses two Spring profiles:
 * <ul>
 *   <li>{@code local}  - points to LocalStack at {@code aws.endpoint-override}</li>
 *   <li>{@code prod}   - uses the default AWS credential chain</li>
 * </ul>
 */
@Slf4j
@Configuration
public class AwsConfig {

    @Value("${aws.region:us-east-1}")
    private String region;

    // local / LocalStack profile

    @Bean
    @Profile("local")
    public S3Client localS3Client(@Value("${aws.endpoint-override}") String endpointOverride) {
        log.info("Configuring S3Client for LOCAL profile -> {}", endpointOverride);
        AwsCredentialsProvider credentials =
                StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));

        return S3Client.builder()
                .endpointOverride(URI.create(endpointOverride))
                .credentialsProvider(credentials)
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    @Bean
    @Profile("local")
    public S3Presigner localS3Presigner(@Value("${aws.endpoint-override}") String endpointOverride) {
        AwsCredentialsProvider credentials =
                StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));

        return S3Presigner.builder()
                .endpointOverride(URI.create(endpointOverride))
                .credentialsProvider(credentials)
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    // Production profile

    @Bean
    @Profile("prod")
    public S3Client prodS3Client() {
        log.info("Configuring S3Client for PROD profile - region={}", region);
        return S3Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.of(region))
                .build();
    }

    @Bean
    @Profile("prod")
    public S3Presigner prodS3Presigner() {
        return S3Presigner.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .region(Region.of(region))
                .build();
    }
}
