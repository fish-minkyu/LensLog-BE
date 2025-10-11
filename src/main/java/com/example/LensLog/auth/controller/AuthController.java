package com.example.LensLog.auth.controller;

import com.example.LensLog.auth.dto.UserDto;
import com.example.LensLog.auth.entity.User;
import com.example.LensLog.auth.jwt.JwtRequestDto;
import com.example.LensLog.auth.jwt.JwtResponseDto;
import com.example.LensLog.auth.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth Controller", description = "사용자 관련 API")
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

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<String> login(
        @RequestBody JwtRequestDto dto,
        HttpServletResponse response
    ) {
        authService.login(dto, response);
        return ResponseEntity.ok("Login Success");
    }

    // JWT 재발급 메서드
    @PostMapping("/refresh")
    public JwtResponseDto reIssueTokens(@RequestBody String refreshToken) {
        return authService.reIssueTokens(refreshToken);
    }

    // JWT 발급 메서드(테스트용)
    @PostMapping("/issue/test")
    public JwtResponseDto issueJwt(@RequestBody JwtRequestDto dto) {
        return authService.issueTokens(dto);
    }
}
