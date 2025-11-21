package com.example.LensLog.config;

import com.example.LensLog.auth.jwt.JwtTokenFilter;
import com.example.LensLog.auth.jwt.JwtTokenUtils;
import com.example.LensLog.auth.oatuh.OAuth2SuccessHandler;
import com.example.LensLog.auth.oatuh.OAuth2UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

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
            // cors 설정
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // JWT를 사용하기 때문에 보안 관련 세션 해제
            .sessionManagement(session -> session
                // 세션을 저장하지 않는다.
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**")
                .permitAll()
                // 전부 허가
                .requestMatchers(HttpMethod.GET,
                    // photo API
                    "/api/photo/getList",
                    "/api/photo/getOne/{photoId}"
                )
                .permitAll()
                .requestMatchers(HttpMethod.POST,
                    // auth API
                    "/api/auth/join",
                    "/api/auth/login"
                    )
                .permitAll()
                // 로그인 권한
                .requestMatchers(HttpMethod.GET,
                    // photo API
                    "/api/photo/download/{photoId}",
                    // Good API
                    "/api/good/{photoId}"
                )
                .authenticated()
                .requestMatchers(HttpMethod.POST,
                    // auth API
                    "/api/auth/refresh"
                    )
                .authenticated()
                // 관리자 권한
                .requestMatchers(HttpMethod.POST,
                    // photo API
                    "/api/photo/upload"
                    )
                .hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE,
                    // photo API
                    "/api/photo/delete/{photoId}"
                    )
                .hasRole("ADMIN")
                // 그 외는 전부 허가
                .anyRequest()
                .permitAll()
            )
            // 인증되지 않은 사용자가 보호된 리소스 접근 시 처리 방식 지정
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            // OAuth
            .oauth2Login(oauth2Login -> oauth2Login
                .successHandler(oAuth2SuccessHandler)
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oAuth2UserService))
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 허용할 Origin
        configuration.setAllowedOrigins(
            Arrays.asList("http://localhost:5173")
        );
        // 허용할 HTTP Method
        configuration.setAllowedMethods(
            Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );
        // 모든 헤더 허용
        configuration.setAllowedHeaders(Collections.singletonList("*"));
        // 인증 정보(쿠키 등) 허용
        configuration.setAllowCredentials(true);
        // 클라이언트에 노출할 헤더
        configuration.setExposedHeaders(
            Arrays.asList("Content-Disposition", "Authorization")
        );

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }
}
