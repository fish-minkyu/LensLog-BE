package com.example.LensLog.auth.service;

import com.example.LensLog.auth.CustomUserDetails;
import com.example.LensLog.auth.dto.UserDto;
import com.example.LensLog.auth.entity.RoleEnum;
import com.example.LensLog.auth.entity.User;
import com.example.LensLog.auth.jwt.JwtTokenUtils;
import com.example.LensLog.auth.repo.UserRepository;
import com.example.LensLog.constant.TokenConstant;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    // 1. 사용자 정보를 가져오기 위한 레포지토리
    private final UserRepository userRepository;
    // 2. JWT를 발급하기 위한 Bean
    private final JwtTokenUtils jwtTokenUtils;
    // 3. 사용자가 제공한 아이디 비밀번호를 비교하기 위한 클래스
    private final PasswordEncoder passwordEncoder;
    // 4. 사용자 정보를 가지고 오는 서비스
    private final UserDetailsService userDetailsService;
    // 5. 비밀번호 양식 확인(최소 8자리, 최소 1개의 대문자, 최소 1개의 특수문자를 필수 포함)
    private static final String PASSWORD_REGEX
        = "^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$";

    public AuthServiceImpl(
        UserRepository userRepository,
        JwtTokenUtils jwtTokenUtils,
        PasswordEncoder passwordEncoder,
        UserDetailsService userDetailsService
    ) {
        this.userRepository = userRepository;
        this.jwtTokenUtils = jwtTokenUtils;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;

        // 관리자 계정 생성
        if (!userExists("admin")) {
            signUp(UserDto.builder()
                .username("admin")
                .password("A123456!")
                .email("admin@lenslong.io")
                .authority(RoleEnum.ROLE_ADMIN.name())
                .build()
            );
        }

        // 일반 사용자 계정 생성
        if (!userExists("user")) {
            signUp(UserDto.builder()
                .username("user")
                .password("U123456!")
                .email("user@lenslong.io")
                .authority(RoleEnum.ROLE_USER.name())
                .build()
            );
        }
    }

    // 회원가입
    @Override
    public UserDto signUp(UserDto dto) {
        // 사용자 이름 중복 검사
        if (this.userExists(dto.getUsername())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "This username already exist");
        }

        // 비밀번호 유효성 검사
        if (!isPasswordValid(dto.getPassword())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Password must be at least 8 characters long. including at least one uppercase letter" +
                    "and special character"
            );
        }

        try {
            User newUser = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword())) // 비밀번호 암호화
                .authority(dto.getAuthority())
                .build();

            userRepository.save(newUser);

            return UserDto.fromEntity(newUser);
        } catch (ClassCastException e) {
            log.error("Failed Cast to: {}", CustomUserDetails.class);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isPasswordValid(String password) {
        return password.matches(PASSWORD_REGEX);
    }

    // 로그인
    @Override
    public void login(UserDto dto, HttpServletResponse response) {
        UserDetails userDetails;

        try {
            // 1. 사용자 정보 조회
            userDetails = userDetailsService.loadUserByUsername(dto.getUsername());
        } catch (UsernameNotFoundException e) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid username or password");
        }

        // 2. 비밀번호 대조
        // => 날 것의 비밀번호와 암호화된 비밀번호를 비교한다.
        if (!passwordEncoder.matches(dto.getPassword(), userDetails.getPassword())) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid username or password");
        }

        // 3. 토큰 발급(Cookie에 추가)
        issueTokens(userDetails, response);
    }

    // Access Token & Refresh Token 발급
    @Override
    public void issueTokens(UserDetails userDetails, HttpServletResponse response) {
        // Access Token 발급
        String jwt = jwtTokenUtils.generateToken(userDetails);

        // Refresth Token 발급
        String refreshToken = jwtTokenUtils.generateRefreshToken(userDetails);

        // Access Token을 쿠키에 담아 응답에 추가한다.
        makeCookie(TokenConstant.ACCESS_TOKEN, jwt, response);
        // Refresh Token을 쿠키에 담아 응답에 추가한다.
        makeCookie(TokenConstant.REFRESH_TOKEN, refreshToken, response);
    }

    // Access Token과 Refresh Token을 재발급
    @Override
    public void reIssueTokens(String refreshToken, HttpServletResponse response) {
        String username;
        String redisKey;

        try {
            username = jwtTokenUtils.extractUsername(refreshToken);
            redisKey = jwtTokenUtils.extractRediskey(refreshToken);
        } catch (ExpiredJwtException e) {
            // Refresh Token 자체도 만료된 경우
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh Token has expired. Please log in again.");
        } catch (IllegalArgumentException e) {
            // 유효하지 않은 Refresh Token (형식 오류)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Refresh Token. Please log in again.");
        }

        // Redis에 저장된 Refresh Token인지 확인
        String storedRefreshToken =
            jwtTokenUtils.getRedisTemplate().opsForValue().get(redisKey);

        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            // Redis에 없거나 일치하지 않는다면 이미 만료되었거나, 탈취된 토큰일 수 있다.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or Revoked Refresh Token. Please log in again.");
        }

        // Refresh Token의 사용자 정보와 UserDetails의 사용자 정보 일치 확인
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtTokenUtils.validateToken(refreshToken, userDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh Token validation failed for user.");
        }

        // 기존 Refresh Token 삭제 (Redis에서)
        jwtTokenUtils.deleteRefreshToken(redisKey);

        // 새로운 Access Token 발급 및 Cookie로 반환
        String newAccessToken = jwtTokenUtils.generateToken(userDetails);
        makeCookie(TokenConstant.ACCESS_TOKEN, newAccessToken, response);

        // 새로운 Refresh Token 발급 및 Cookie로 반환
        String newRefreshToken = jwtTokenUtils.generateRefreshToken(userDetails);
        makeCookie(TokenConstant.REFRESH_TOKEN, newRefreshToken, response);
    }

    // 사용자 존재 유무 확인
    @Override
    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }

    // 사용자 존재 유무 email로 확인
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // 쿠키를 만들어 응답에 넣는 메서드
    private void makeCookie(String cookieName, String token, HttpServletResponse response) {
        Cookie cookie = new Cookie(cookieName, token);
        cookie.setHttpOnly(true); // XSS 공격 방지
        cookie.setSecure(false);  // HTTPS에서만 전송 (네트워크 스니핑 방지) -> 개발 중이어서 false
        cookie.setPath("/"); // 모든 경로에서 접근 가능

        response.addCookie(cookie);
    }

    // 쿠키 삭제 메서드
    private void deleteCookie(String cookieName, HttpServletResponse response) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");

        response.addCookie(cookie);
    }
}
