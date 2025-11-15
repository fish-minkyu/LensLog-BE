package com.example.LensLog.auth.oatuh;

import com.example.LensLog.auth.dto.UserDto;
import com.example.LensLog.auth.repo.UserRepository;
import com.example.LensLog.auth.service.AuthService;
import com.example.LensLog.auth.service.JpaUserDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
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

    // 사용자 정보를 가지고 오는 서비스
    private final JpaUserDetailsService userDetailsService;
    // JWT 토큰 발급을 위해 AuthService
    private final AuthService authService;
    // 비밀번호 암호화 객체
    private final PasswordEncoder passwordEncoder;
    // 사용자 DB를 관리하는 repo
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        // OAuth2UserServiceImpl의 반환값이 할당된다.
        OAuth2User oAuth2User
            = (OAuth2User) authentication.getPrincipal();

        // 해당 username이 이미 가입이 되어있는지 확인하기 위해 꺼낸다.
        String provider = oAuth2User.getAttribute("provider");
        String username = oAuth2User.getAttribute("nickname");

        // 처음으로 이 소셜 로그인으로 로그인을 시도했다.
        if (!userRepository.existsUserWithProvider(provider, username)) {
            // 넘겨받은 정보를 바탕으로 사용자 정보를 준비한다.
            String providerId = oAuth2User.getAttribute("id").toString();
            String name = oAuth2User.getAttribute("name");
            String email = oAuth2User.getAttribute("email");

            // 새 계정을 만든다.
            authService.signUp(UserDto.builder()
                .username(username)
                .password(passwordEncoder.encode(providerId))
                .name(name)
                .email(email)
                .provider(provider)
                .authority("ROLE_USER")
                .build());
        }

        // 데이터베이스에서 사용자 계정 회수
        UserDetails userDetails
            = userDetailsService.loadUserByUsername(username);

        // Access Token & Refresh Token 생성 및 쿠키 설정
        authService.issueTokens(userDetails, response);

        // 인증 성공 시, 사용자를 특정 URL로 리다리렉트하기.
        String targetUrl = determineTargetUrl(request, response, authentication);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    // Redirect할 기본 URL 설정
    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        return "http://localhost:5173/";
    }
}
