package com.palmera_junior.gestion_compras.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@Configuration
public class SesConfig {

    @Bean
    AwsCredentialsProvider sesCredentialsProvider(
            @Value("${aws.ses.access-key-id:}") String accessKeyId,
            @Value("${aws.ses.secret-access-key:}") String secretAccessKey) {
        if (accessKeyId.isBlank() || secretAccessKey.isBlank()) {
            return DefaultCredentialsProvider.create();
        }

        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey));
    }

    @Bean(destroyMethod = "close")
    SesV2Client sesV2Client(
            @Value("${aws.ses.region:us-east-1}") String region,
            AwsCredentialsProvider sesCredentialsProvider) {
        return SesV2Client.builder()
                .region(Region.of(region))
                .credentialsProvider(sesCredentialsProvider)
                .build();
    }
}
