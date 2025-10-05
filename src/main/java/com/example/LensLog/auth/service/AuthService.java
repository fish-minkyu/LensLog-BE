package com.example.LensLog.auth.service;

import com.example.LensLog.auth.entity.User;
import com.example.LensLog.auth.jwt.JwtRequestDto;
import com.example.LensLog.auth.jwt.JwtResponseDto;

public interface AuthService {
    // 회원가입
    User signUp(User user);

    // 로그인
    JwtResponseDto login(JwtRequestDto dto);
}
