package com.example.LensLog.auth.service;

import com.example.LensLog.auth.dto.UserDto;
import com.example.LensLog.auth.entity.User;
import com.example.LensLog.auth.jwt.JwtRequestDto;
import com.example.LensLog.auth.jwt.JwtResponseDto;
import com.example.LensLog.auth.jwt.RefreshTokenDto;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    // 회원가입
    UserDto signUp(User user);

    // Access Token & Refresh Token 발급
    JwtResponseDto issueTokens(JwtRequestDto dto);

    // 사용자 존재 유무 확인
    boolean userExists(String username);

    // 로그인
    void login(JwtRequestDto dto, HttpServletResponse response);

    // 토큰 재발급
    JwtResponseDto reIssueTokens(RefreshTokenDto dto);

    // 사용자 존재 유무 email로 확인
    boolean existsByEmail(String email);
}
