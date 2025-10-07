package com.example.LensLog.auth.controller;

import com.example.LensLog.auth.dto.UserDto;
import com.example.LensLog.auth.entity.User;
import com.example.LensLog.auth.jwt.JwtRequestDto;
import com.example.LensLog.auth.jwt.JwtResponseDto;
import com.example.LensLog.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    // 회원 가입
    @PostMapping("/join")
    public UserDto signUp(@RequestBody User user) {
        return authService.signUp(user);
    }

    // JWT 발급 메서드(로그인)
    @PostMapping("/issue")
    public JwtResponseDto issueJwt(@RequestBody JwtRequestDto dto) {
        return authService.login(dto);
    }

    // JWT 재발급 메서드
    @PostMapping("/refresh")
    public JwtResponseDto reIssueTokens(@RequestBody String refreshToken) {
        return authService.reIssueTokens(refreshToken);
    }
}
