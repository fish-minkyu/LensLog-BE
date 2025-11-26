package com.example.LensLog.common;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RateLimiterInterceptor implements HandlerInterceptor {
    // 초당 10회 제한 설정 주입
    private final BucketConfiguration bucketConfiguration;
    // Redis 통신을 위한 매니저 주입
    private final LettuceBasedProxyManager proxyManager;
    private final String UNKNOWN = "unknown";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 클라이언트의 IP 주소를 Key로 사용
        String ipAddress = getClientIp(request);
        if (StringUtils.isBlank(ipAddress)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "IP 주소를 찾을 수 없습니다."
            );
        }

        return checkBucketCounter(ipAddress);
    }

    private boolean checkBucketCounter(final String key) {
        final Bucket bucket = bucket(key);
        // 토큰 소비 시도
        if (!bucket.tryConsume(1)) {
            // 토큰 소비 실패 (Rate Limit 초과)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        }

        // 토큰 소비 성공 (요청 허용)
        return true;
    }

    private Bucket bucket(final String key) {
        return proxyManager.builder().build(key.getBytes(), bucketConfiguration);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StringUtils.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (StringUtils.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }
}
