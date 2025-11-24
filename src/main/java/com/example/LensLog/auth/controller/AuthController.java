package com.example.LensLog.auth.controller;

import com.example.LensLog.auth.dto.PasswordDto;
import com.example.LensLog.auth.dto.UserDto;
import com.example.LensLog.auth.service.AuthService;
import com.example.LensLog.constant.TokenConstant;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public UserDto login(
        @RequestBody UserDto dto,
        HttpServletResponse response
    ) {
        return authService.login(dto, response);
    }

    // 비밀번호 찾기 인증
    @PostMapping("/verification/password")
    public boolean verificationPassword(@RequestBody UserDto dto) {
        return authService.verificationPassword(dto);
    }

    // 비밀번호 변경
    @PutMapping("/change/password")
    public ResponseEntity<String> changePassword(@RequestBody PasswordDto dto) {

        return ResponseEntity.ok("Changing password is success.");
    }

    // 사용자 username 찾기
    @GetMapping("/find/username")
    public List<UserDto> findUsername(
        @RequestParam String name,
        @RequestParam String email
    ) {
        return authService.findUsername(name, email);
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
        @CookieValue(name = TokenConstant.REFRESH_TOKEN) String refreshToken,
        HttpServletResponse response
    ) {
        authService.logout(refreshToken, response);
        return ResponseEntity.ok("Logout is success.");
    }

    // 회원탈퇴
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteUser(
        @CookieValue(name = TokenConstant.REFRESH_TOKEN) String refreshToken,
        @RequestBody UserDto dto,
        HttpServletResponse response
    ) {
        authService.deleteUser(refreshToken, dto.getPassword(), response);
        return null;
    }

    // JWT 재발급 메서드
    @PostMapping("/refresh")
    public UserDto reIssueTokens(
        @CookieValue(name = TokenConstant.REFRESH_TOKEN) String refreshToken,
        HttpServletResponse response
    ) {
        return authService.reIssueTokens(refreshToken, response);
    }

    // 유저 정보 반환
    @GetMapping("/checkLogin")
    public UserDto checkLogin() {
        return authService.checkLogin();
    }
}
