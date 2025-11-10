package com.example.LensLog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // 해당 URL에 대한 CORS 허용
            .allowedOrigins("http://localhost:3000") // React 개발 주소
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*") // 모든 헤더 허용
            .allowCredentials(true); //쿠키와 인증 정보 허용
    }
}
