package com.example.LensLog.auth.controller;

import com.example.LensLog.auth.dto.UserDto;
import com.example.LensLog.auth.service.AuthService;
import com.example.LensLog.constant.TokenConstant;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth Controller", description = "사용자 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    // 회원 가입
    @PostMapping("/join")
    public UserDto signUp(@RequestBody UserDto dto) {
        return authService.signUp(dto);
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<String> socialLogin(
        @RequestBody UserDto dto,
        HttpServletResponse response
    ) {
        authService.login(dto, response);
        return ResponseEntity.ok("Login Success");
    }

    // JWT 재발급 메서드
    @PostMapping("/refresh")
    public ResponseEntity<String> reIssueTokens(
        @CookieValue(name = TokenConstant.REFRESH_TOKEN) String refreshToken,
        HttpServletResponse response
    ) {
        authService.reIssueTokens(refreshToken, response);
        return ResponseEntity.ok("Tokens re-issue success");
    }
}
