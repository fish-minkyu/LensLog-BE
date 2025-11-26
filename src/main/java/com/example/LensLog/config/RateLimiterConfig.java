package com.example.LensLog.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimiterConfig {
    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Bean
    public RedisClient redisClient() {
        final RedisURI redisURI = RedisURI.builder()
            .withHost(redisHost)
            .withPort(redisPort)
            .withPassword(redisPassword.toCharArray())
            .build();

        return RedisClient.create(redisURI);
    }

    @Bean
    public LettuceBasedProxyManager lettuceBasedProxyManager() {
        return LettuceBasedProxyManager
            .builderFor(redisClient())
            .withExpirationStrategy(ExpirationAfterWriteStrategy
                .basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(1)))
            .build();
    }

    @Bean
    public BucketConfiguration bucketConfiguration() {
        // 초당 1개의 토큰을 리필하고, 최대 1개까지 저장할 수 있는 설정
        // 초당 5회 요청 제한
        Refill refill = Refill.intervally(5, Duration.ofSeconds(1));
        Bandwidth limit = Bandwidth.classic(5, refill);
        return BucketConfiguration.builder().addLimit(limit).build();
    }
}
