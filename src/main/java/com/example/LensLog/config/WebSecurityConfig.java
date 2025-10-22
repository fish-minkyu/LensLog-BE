package com.example.LensLog.config;

import com.example.LensLog.auth.jwt.JwtTokenFilter;
import com.example.LensLog.auth.jwt.JwtTokenUtils;
import com.example.LensLog.auth.oatuh.OAuth2SuccessHandler;
import com.example.LensLog.auth.oatuh.OAuth2UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@RequiredArgsConstructor
public class WebSecurityConfig {
    private final JwtTokenUtils jwtTokenUtils;
    private final UserDetailsService manager;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2UserServiceImpl oAuth2UserService;

    // Http 관련 보안 설정하는 객체
    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http
    ) throws Exception {
        http
            // csrf 보안 해제
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // 전부 허가
                .requestMatchers(HttpMethod.GET,
                    "/photo/getList",
                    "/photo/getOne/{photoId}"
                )
                .permitAll()
                // 익명 사용자 권한
                .requestMatchers(HttpMethod.POST,
                    "/auth/join",
                    "/auth/issue"
                    )
                .anonymous()
                // 로그인 권한
                .requestMatchers(HttpMethod.GET,
                    "/photo/download/{photoId}",
                    "/like/good",
                    "/like/delete"
                )
                .authenticated()
                .requestMatchers(HttpMethod.POST,
                    "/auth/refresh"
                    )
                .authenticated()
                // 관리자 권한
                .requestMatchers(HttpMethod.POST,
                    "/photo/upload"
                    )
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE,
                    "/photo/delete/{photoId}"
                    )
                .hasRole("ADMIN")
                // 그 외는 전부 허가
                .anyRequest()
                .permitAll()
            )
            // OAuth
            .oauth2Login(oauth2Login -> oauth2Login
                .loginPage("/auth/social/login")
                .successHandler(oAuth2SuccessHandler)
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oAuth2UserService))
            )

            // JWT를 사용하기 때문에 보안 관련 세션 해제
            .sessionManagement(session -> session
                // 세션을 저장하지 않는다.
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // JWT 필터를 권한 필터 앞에 삽입
            .addFilterBefore(
                new JwtTokenFilter(
                    jwtTokenUtils,
                    manager
                ),
                AuthorizationFilter.class
            );

        return http.build();
    }
}
