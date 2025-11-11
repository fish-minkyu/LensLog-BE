package com.example.LensLog.auth.oatuh;

import com.example.LensLog.auth.CustomUserDetails;
import com.example.LensLog.auth.dto.UserDto;
import com.example.LensLog.auth.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// OAuth2UserServiceImpl이 성공적으로 OAuth2 과정을 마무리 했을 때,
// 넘겨받은 사용자 정보를 바탕으로 jwt를 생성하고 클라이언트에 전달
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler
    // 인증에 성공했을 때, 특정 URL로 리다이렉트를 하고 싶은 경우 활용 가능한 SuccessHandler
    extends SimpleUrlAuthenticationSuccessHandler {

    // JWT 토큰 발급을 위해 AuthService
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
//        String username = String.format("{%s}%s", provider, email);
        String providerId = oAuth2User.getAttribute("id").toString();

        // 처음으로 이 소셜 로그인으로 로그인을 시도했다.
        if (!authService.existsByEmail(email)) {
            // 새 계정을 만든다.
            authService.signUp(UserDto.builder()
                .email(email)
                .password(passwordEncoder.encode(providerId))
                .provider(provider)

                //TODO 사용자 이름과 생년 월일도 추가 필요
                .build());
        }

        // 데이터베이스에서 사용자 계정 회수
        CustomUserDetails customUserDetails
            = authService.loadUserByEmail(email);

        // Access Token & Refresh Token 생성 및 쿠키 설정
        authService.issueTokens(customUserDetails, response);

        // 인증 성공 시, 사용자를 특정 URL로 리다리렉트하기.
        String targetUrl = determineTargetUrl(request, response, authentication);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    // Redirect할 기본 URL 설정
    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        //TODO application.yml에서 따로 설정해주기
        // React 페이지 url을 반환해야 한다.
        return "http://localhost:8080/oauth2/redirect";
    }
}
