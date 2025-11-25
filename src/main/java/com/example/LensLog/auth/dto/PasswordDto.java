package com.example.LensLog.auth.dto;

import lombok.Getter;

@Getter
public class PasswordDto {
    private String username;
    private String email;
    private String provider;
    private String currentPassword;
    private String changePassword1;
    private String changePassword2;
    private String recaptchaResponse; // reCAPTCHA 응답 토큰
}
