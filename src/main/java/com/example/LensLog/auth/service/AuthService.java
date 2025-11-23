package com.example.LensLog.auth.service;

import com.example.LensLog.auth.dto.PasswordDto;
import com.example.LensLog.auth.dto.UserDto;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthService {
    // 회원가입
    UserDto signUp(UserDto dto);

    // Access Token & Refresh Token 발급
    void issueTokens(UserDetails userDetails, HttpServletResponse response);

    // 로그인
    UserDto login(UserDto dto, HttpServletResponse response);

    // 토큰 재발급
    UserDto reIssueTokens(String refreshToken, HttpServletResponse response);

    // 사용자 존재 유무 email로 확인
    boolean existsByEmail(String email);

    // 비밀번호 변경
    void changePassword(PasswordDto dto);

    // 사용자 username 찾기
    UserDto findUsername(String name, String email);

    // 로그아웃
    void logout(String refreshToken, HttpServletResponse response);

    // 회원탈퇴
    void deleteUser(String refreshToken, String password, HttpServletResponse response);

    // 유저 정보 반환
    UserDto checkLogin();
}
