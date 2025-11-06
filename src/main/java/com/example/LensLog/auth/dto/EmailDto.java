package com.example.LensLog.auth.dto;

import lombok.Getter;

@Getter
public class EmailDto {
    private String email;
    private String verifyCode; // 인증 코드
}
