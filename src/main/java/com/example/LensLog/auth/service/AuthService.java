package com.example.LensLog.auth.service;

import com.example.LensLog.auth.dto.UserDto;
import com.example.LensLog.auth.entity.User;
import com.example.LensLog.auth.jwt.JwtRequestDto;
import com.example.LensLog.auth.jwt.JwtResponseDto;

public interface AuthService {
    // 회원가입
    UserDto signUp(User user);

    // 로그인
    JwtResponseDto login(JwtRequestDto dto);
}
