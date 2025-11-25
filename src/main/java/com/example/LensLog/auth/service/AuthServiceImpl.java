package com.example.LensLog.auth.service;

import com.example.LensLog.auth.CustomUserDetails;
import com.example.LensLog.auth.dto.PasswordDto;
import com.example.LensLog.auth.dto.RecaptchaResponseDto;
import com.example.LensLog.auth.dto.UserDto;
import com.example.LensLog.auth.entity.RoleEnum;
import com.example.LensLog.auth.entity.User;
import com.example.LensLog.auth.jwt.JwtTokenUtils;
import com.example.LensLog.auth.repo.UserRepository;
import com.example.LensLog.common.AuthenticationFacade;
import com.example.LensLog.constant.LoginTypeConstant;
import com.example.LensLog.constant.TokenConstant;
import io.jsonwebtoken.ExpiredJwtException;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    // 1. 사용자 정보를 가져오기 위한 레포지토리
    private final UserRepository userRepository;
    // 2. JWT를 발급하기 위한 Bean
    private final JwtTokenUtils jwtTokenUtils;
    // 3. 사용자가 제공한 아이디 비밀번호를 비교하기 위한 클래스
    private final PasswordEncoder passwordEncoder;
    // 4. 사용자 정보를 가지고 오는 서비스
    private final JpaUserDetailsService userDetailsService;
    // 5. Refresh Token이 저장된 Redis
    private final StringRedisTemplate redisTemplate;
    // 6. email 관련 로직을 처리하는 EmailService
    private final EmailService emailService;
    // 7. 사용자 인증 정보를 가지고 오는 Util 메서드
    private final AuthenticationFacade auth;
    // 8. Recaptcha 인증을 확인하기 위한 서비스
    private final RecaptchaService recaptchaService;
    // 9. 비밀번호 양식 확인(최소 8자리, 최소 1개의 대문자, 최소 1개의 특수문자를 필수 포함)
    private static final String PASSWORD_REGEX
        = "^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$";

    // ReCAPTCHA v3 점수 임계값 설정
    // 기본값은 0.5로 설정, 필요에 따라 조정
    @Value("${recaptcha.threshold:0.5}")
    private float recaptchaThreshold;

    // 회원가입
    @Override
    public UserDto signUp(UserDto dto) {
        // 사용자 ID가 이미 있는지 확인
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "This username is already exist");
        }

        // 비밀번호 유효성 검사
        if (!isPasswordValid(dto.getPassword())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Password must be at least 8 characters long. including at least one uppercase letter" +
                    "and special character"
            );
        }

        if (StringUtils.isBlank(dto.getProvider())) {
            dto.setProvider(LoginTypeConstant.LOCAL);
        }

        // 이메일 인증 코드 검증
        if (!emailService.verificationCode(dto.getProvider(), dto.getEmail(), dto.getVerifyCode())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid or expired verification code for email" + dto.getEmail()
            );
        }

        try {
            User newUser = User.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword())) // 비밀번호 암호화
                .name(dto.getName())
                .email(dto.getEmail())
                .isVerified(Boolean.TRUE)
                .provider(dto.getProvider())
                .authority(RoleEnum.ROLE_USER.name())
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
    public UserDto login(UserDto dto, HttpServletResponse response) {
        CustomUserDetails userDetails;

        try {
            // 1. 사용자 정보 조회
            userDetails
                = (CustomUserDetails) userDetailsService.loadUserByUsername(dto.getUsername());
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

        UserDto result = new UserDto();
        result.setUsername(userDetails.getUsername());
        result.setProvider(userDetails.getProvider());
        result.setAuthority(userDetails.getAuthorities().toString());

        return result;
    }

    // Access Token & Refresh Token 발급
    @Override
    public void issueTokens(UserDetails userDetails, HttpServletResponse response) {
        // Access Token 발급, Access Token을 쿠키에 담아 응답에 추가
        String jwt = jwtTokenUtils.generateToken(userDetails);
        makeCookie(TokenConstant.ACCESS_TOKEN, jwt, response);

        // Refresth Token 발급, Refresh Token을 쿠키에 담아 응답에 추가
        String refreshToken = jwtTokenUtils.generateRefreshToken(userDetails);
        makeCookie(TokenConstant.REFRESH_TOKEN, refreshToken, response);
    }

    // Access Token과 Refresh Token을 재발급
    @Override
    public UserDto reIssueTokens(String refreshToken, HttpServletResponse response) {
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
            redisTemplate.opsForValue().get(redisKey);

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

        UserDto result = new UserDto();
        result.setUsername(userDetails.getUsername());
        result.setAuthority(userDetails.getAuthorities().toString());
        return result;
    }

    // 비밀번호 찾기 인증
    @Override
    public void verificationPassword(UserDto dto) {
        // 해당 계정이 있는지 확인
        if (!userRepository.existsUsernameWithEmail(dto.getUsername(), dto.getEmail())) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "일치하는 사용자 계정이 없습니다."
            );
        }

        // 이메일 인증
        if (!emailService.verificationCode(dto.getProvider(), dto.getEmail(), dto.getVerifyCode())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "일치하는 인증번호가 없습니다."
            );
        }
    }

    // 비밀번호 변경
    @Override
    public void changePassword(PasswordDto dto) {
        // 비밀번호 찾는 계정 찾기
        User user = userRepository.findOnlyOneUser(dto.getProvider(), dto.getUsername(), dto.getEmail())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 사용자 계정이 없습니다."));

        String provider = user.getProvider();
        if (StringUtils.isBlank(provider)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Provider는 null이 될 수 없습니다."
            );
        }

        // 소셜 로그인일 경우, Bad Request
        if (!LoginTypeConstant.LOCAL.equals(provider)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "소셜 로그인은 비밀번호를 변경할 수 없습니다."
            );
        }

        // Recaptcha 검증 로직
        if (StringUtils.isBlank(dto.getRecaptchaResponse())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "reCAPTCHA 인증 정보가 필요합니다."
            );
        }

        RecaptchaResponseDto recaptchaResult
            = recaptchaService.verifyRecaptcha(dto.getRecaptchaResponse());

        if (recaptchaResult == null || !recaptchaResult.isSuccess()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "reCAPTCHA 검증 서비스에 문제가 발생했습니다. 다시 시도해주세요."
            );
        }

        // v3: 점수를 확인하여 봇 여부 판단
        if (recaptchaResult.getScore() < recaptchaThreshold) {
            // 점수가 낮으면 봇으로 판단
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "로봇 활동으로 의심됩니다. 잠시 후 다시 시도해주세요."
            );
        }

        // 바꿀 비밀번호와 확인용 비밀번호가 일치하는지 확인
        String pwd1 = dto.getChangePassword1();
        String pwd2 = dto.getChangePassword2();
        // pwd1와 pwd2가 null이 아니어야 하고
        if (StringUtils.isNotBlank(pwd1) && StringUtils.isNotBlank(pwd2)) {
            // pwd1와 pwd2가 서로 같지 않다면 에러를 반환한다.
            if (!pwd1.equals(pwd2)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "The password didn't match it"
                );
            }
        } else {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "입력값을 다시 확인해주세요."
            );
        }

        // 새 비밀번호 유효성 검사
        if (!isPasswordValid(dto.getChangePassword1())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "비밀번호 규칙에 어긋납니다."
            );
        }

        // 비밀번호 변경
        String encodedNewPassword = passwordEncoder.encode(dto.getChangePassword1());
        user.setPassword(encodedNewPassword);

        userRepository.save(user);
    }

    // 사용자 username 찾기
    @Override
    public List<UserDto> findUsername(String name, String email) {
        List<User> userList = userRepository.findNameWithEmail(name, email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "There is no ID."
            ));

        List<UserDto> result = new ArrayList<>();
        for (User user : userList) {
            UserDto userDto = UserDto.builder()
                .provider(user.getProvider())
                .username(user.getUsername())
                .name(user.getName())
                .createdDate(user.getCreatedDate())
                .build();

            result.add(userDto);
        }

        return result;
    }

    // 로그아웃
    @Override
    public void logout(String refreshToken, HttpServletResponse response) {
        // 인증 확인
        User user = auth.getAuth();

        // Redis에 저장된 Refresh Token 삭제
        String redisKey = jwtTokenUtils.extractRediskey(refreshToken);

        // Spring Security Context Clear
        SecurityContextHolder.clearContext();

        // 클라이언트의 HttpOnly 쿠키에 토큰들이 저장되어 있다면 삭제
        deleteCookie(TokenConstant.ACCESS_TOKEN, response);
        deleteCookie(TokenConstant.REFRESH_TOKEN, response);
    }

    // 회원탈퇴
    @Override
    public void deleteUser(String refreshToken, String password, HttpServletResponse response) {
        // 인증 확인
        User user = auth.getAuth();

        // 비밀번호 확인
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }

        // 로그아웃 진행
        logout(refreshToken, response);

        // 사용자 데이터 삭제
        userRepository.delete(user);
    }

    // 사용자 존재 유무 email로 확인
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // 쿠키를 만들어 응답에 넣는 메서드
    private void makeCookie(String name, String token, HttpServletResponse response) {
        Cookie cookie = new Cookie(name, token);
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

    // 유저 정보 반환
    @Override
    public UserDto checkLogin() {
        User user = auth.getAuth();
        return UserDto.fromEntity(user);
    }
}
