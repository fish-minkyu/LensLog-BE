package com.example.LensLog.auth.oatuh;

import com.example.LensLog.auth.entity.User;
import com.example.LensLog.auth.jwt.JwtTokenUtils;
import com.example.LensLog.auth.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// OAuth2UserServiceImpl이 성공적으로 OAuth2 과정을 마무리 했을 때,
// 넘겨받은 사용자 정보를 바탕으로 JWT를 생성,
// 클라이언트한테 JWT를 전달
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler
    // 인증에 성공했을 때, 특정 URL로 리다이렉트를 하고 싶은 경우 활용 가능한 SuccessHandler
    extends SimpleUrlAuthenticationSuccessHandler {

    // JWT 발급을 위해 JwtTokenUtils 필요
    private final JwtTokenUtils jwtTokenUtils;
    // 사용자 정보 등록을 위해 UserDetailService
    private final UserDetailsService userDetailsService;
    // 사용자 정보를 email로 조회하기 위해 AuthService
    private final AuthService authService;
    //
    private final PasswordEncoder passwordEncoder;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        // OAuth2UserServiceImpl의 반환값이 할당된다.
        OAuth2User oAuth2User
            = (OAuth2User) authentication.getPrincipal();

        // 넘겨받은 정보를 바탕으로 사용자 정보를 준비한다.
        String email = oAuth2User.getAttribute("email");
        String provider = oAuth2User.getAttribute("provider");
        String username = String.format("{%s}%s", provider, email);
        String providerId = oAuth2User.getAttribute("id").toString();

        //TODO email 중복 검사 로직 만들기

        // 처음으로 이 소셜 로그인으로 로그인을 시도했다.
        if (authService.userExists(username)) {
            // 새 계정을 만든다.
            authService.signUp(User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(providerId))
                .build());
        }

        // 데이터베이스에서 사용자 계정 회수
        UserDetails userDetails
            = userDetailsService.loadUserByUsername(username);

        // Access Token 생성 및 쿠키 설정
        String accessToken = jwtTokenUtils.generateToken(userDetails);
        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
        accessTokenCookie.setHttpOnly(true); // XSS 공격 방지
        accessTokenCookie.setSecure(true); // HTTPS에서만 전송 (네트워크 스니핑 방지)
        accessTokenCookie.setPath("/"); // 모든 경로에서 쿠키 접근 가능

        response.addCookie(accessTokenCookie); // 응답에 Access Token 쿠키 추가

        // Refresh Token 생성 및 쿠키 설정
        String refreshToken = jwtTokenUtils.generateRefreshToken(userDetails);
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true); // XSS 공격 방지
        refreshTokenCookie.setSecure(true); // HTTPS에서만 전송 (네트워크 스니핑 방지)
        refreshTokenCookie.setPath("/"); // 모든 경로에서 쿠키 접근 가능
        response.addCookie(refreshTokenCookie); // 응답에 Refresh Token 쿠키 추가

        // 인증 성공 시, 사용자를 특정 URL로 리다리렉트하기.
        String targetUrl = determineTargetUrl(request, response, authentication);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    // Redirect할 기본 URL 설정
    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        //TODO application.yml에서 따로 설정해주기
        return "http://localhost:8080/oauth2/redirect";
    }
}
