package com.example.LensLog.auth.service;

import com.example.LensLog.auth.CustomUserDetails;
import com.example.LensLog.auth.dto.UserDto;
import com.example.LensLog.auth.entity.RoleEnum;
import com.example.LensLog.auth.entity.User;
import com.example.LensLog.auth.jwt.JwtRequestDto;
import com.example.LensLog.auth.jwt.JwtResponseDto;
import com.example.LensLog.auth.jwt.JwtTokenUtils;
import com.example.LensLog.auth.repo.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    // 1. 유저 정보를 가져오기 위한 레포지토리
    private final UserRepository userRepository;
    // 2. JWT를 발급하기 위한 Bean
    private final JwtTokenUtils jwtTokenUtils;
    // 3. 사용자가 제공한 아이디 비밀번호를 비교하기 위한 클래스
    private final PasswordEncoder passwordEncoder;
    // 4. 사용자 정보를 가지고 오는 서비스
    private final UserDetailsService userDetailsService;

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
            signUp(User.builder()
                .username("admin")
                .password("a123")
                .authority(RoleEnum.ROLE_ADMIN.name())
                .build()
            );
        }

        // 일반 사용자 계정 생성
        if (!userExists("user")) {
            signUp(User.builder()
                .username("user")
                .password("u123")
                .authority(RoleEnum.ROLE_USER.name())
                .build()
            );
        }
    }

    // 회원가입
    @Override
    public UserDto signUp(User user) {
        if (this.userExists(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        try {
            User newUser = User.builder()
                .username(user.getUsername())
                .password(passwordEncoder.encode(user.getPassword())) // 비밀번호 암호화
                .authority(user.getAuthority())
                .build();

            userRepository.save(newUser);

            return UserDto.fromEntity(newUser);
        } catch (ClassCastException e) {
            log.error("Failed Cast to: {}", CustomUserDetails.class);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 로그인
    @Override
    public JwtResponseDto login(JwtRequestDto dto) {
        // 1. 사용자가 제공한 username이 저장된 사용자인지 판단
        if (!this.userExists(dto.getUsername())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(dto.getUsername());

        // 2. 비밀번호 대조
        // => 날 것의 비밀번호와 암호화된 비밀번호를 비교한다.
        if (!passwordEncoder.matches(dto.getPassword(), userDetails.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        // 3. Access Token 발급
        String jwt = jwtTokenUtils.generateToken(userDetails);

        // 4. Refresth Token 발급
        String refreshToken = jwtTokenUtils.generateRefreshToken(userDetails);

        return JwtResponseDto.builder()
            .accessToken(jwt)
            .refreshToken(refreshToken)
            .build();
    }

    // Access Token과 Refresh Token을 재발급
    @Override
    public JwtResponseDto reIssueTokens(String refreshToken) {
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

        // 새로운 Access Token 발급
        String newAccessToken = jwtTokenUtils.generateToken(userDetails);

        // 새로운 Refresh Token 발급 및 Redis에 저장
        String newRefreshToken = jwtTokenUtils.generateRefreshToken(userDetails);

        return JwtResponseDto.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .build();
    }

    // 사용자 존재 유무 확인
    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }
}
