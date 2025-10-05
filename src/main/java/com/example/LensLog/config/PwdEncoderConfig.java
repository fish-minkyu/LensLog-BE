package com.example.LensLog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class PwdEncoderConfig {
    @Bean
    public PasswordEncoder PwdEncoderConfig() {
        return new BCryptPasswordEncoder();
    }
}