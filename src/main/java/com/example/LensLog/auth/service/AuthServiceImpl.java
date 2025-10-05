package com.example.LensLog.auth.service;

import com.example.LensLog.auth.CustomUserDetails;
import com.example.LensLog.auth.entity.User;
import com.example.LensLog.auth.jwt.JwtRequestDto;
import com.example.LensLog.auth.jwt.JwtResponseDto;
import com.example.LensLog.auth.jwt.JwtTokenUtils;
import com.example.LensLog.auth.repo.UserRepository;
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
                .build()
            );
        }
    }

    // 회원가입
    @Override
    public User signUp(User user) {
        if (this.userExists(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        try {
            User newUser = User.builder()
                .username(user.getUsername())
                .password(passwordEncoder.encode(user.getPassword())) // 비밀번호 암호화
                .build();

            return userRepository.save(newUser);
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
        if (passwordEncoder.matches(dto.getPassword(), userDetails.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        // 3. JWT 발급
        String jwt = jwtTokenUtils.generateToken(userDetails);

        return JwtResponseDto.builder()
            .token(jwt)
            .build();
    }

    // 사용자 존재 유무 확인
    public boolean userExists(String username) {
        return userRepository.existsByUsername(username);
    }
}
