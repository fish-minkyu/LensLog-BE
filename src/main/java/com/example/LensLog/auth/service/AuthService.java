package com.example.LensLog.auth.service;

import com.example.LensLog.auth.dto.UserDto;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthService {
    // 회원가입
    UserDto signUp(UserDto dto);

    // Access Token & Refresh Token 발급
    void issueTokens(UserDetails userDetails, HttpServletResponse response);

    // 사용자 존재 유무 확인
    boolean userExists(String username);

    // 로그인
    void login(UserDto dto, HttpServletResponse response);

    // 토큰 재발급
    void reIssueTokens(String refreshToken, HttpServletResponse response);

    // 사용자 존재 유무 email로 확인
    boolean existsByEmail(String email);
}
