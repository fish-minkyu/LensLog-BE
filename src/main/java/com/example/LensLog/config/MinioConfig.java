package com.example.LensLog.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {
    @Value("${minio.url}")
    private String url;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
            .endpoint(url)
            .build();
    }
}