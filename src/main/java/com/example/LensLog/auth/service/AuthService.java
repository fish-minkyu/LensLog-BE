package com.example.LensLog.auth.service;

import com.example.LensLog.auth.CustomUserDetails;
import com.example.LensLog.auth.dto.PasswordDto;
import com.example.LensLog.auth.dto.UserDto;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    // 회원가입
    UserDto signUp(UserDto dto);

    // 사용자 정보 조회
    CustomUserDetails loadUserByEmail(String email);

    // Access Token & Refresh Token 발급
    void issueTokens(CustomUserDetails customUserDetails, HttpServletResponse response);

    // 로그인
    void login(UserDto dto, HttpServletResponse response);

    // 토큰 재발급
    void reIssueTokens(String refreshToken, HttpServletResponse response);

    // 사용자 존재 유무 email로 확인
    boolean existsByEmail(String email);

    // 비밀번호 변경
    void changePassword(PasswordDto dto);

    // 로그아웃
    void logout(String refreshToken, HttpServletResponse response);

    // 회원탈퇴
    void deleteUser(String refreshToken, String password, HttpServletResponse response);
}
