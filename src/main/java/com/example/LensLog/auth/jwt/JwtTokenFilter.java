package com.example.LensLog.auth.jwt;

import com.example.LensLog.constant.TokenConstant;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


// 인증 관련 객체는 Bean 객체로 등록하지 않는다. (Bean 객체로 등록하면 자동으로 Filter로 등록이 된다.)
// Why? WebSecurityConfig에서 수동으로1 등록을 해줘야 한다.
// 근데 Bean으로 등록을 해주면 Spring Container가 한번 더 등록하고 Security에서 등록을 하게되면 2번 등록하게 된다.
@Slf4j
public class JwtTokenFilter extends OncePerRequestFilter {
    private final JwtTokenUtils jwtTokenUtils;
    private final UserDetailsService manager;
    private final StringRedisTemplate redisTemplate;
    private static final String REFRESH_ENDPOINT = "/api/auth/refresh";

    public JwtTokenFilter(
        JwtTokenUtils jwtTokenUtils,
        UserDetailsService manager,
        StringRedisTemplate redisTemplate
    ) {
        this.jwtTokenUtils = jwtTokenUtils;
        this.manager = manager;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        log.debug("try jwt filter");

        // 1. auth_token으로 명시된 Access Token 가지고 오기
        String jwtToken = jwtTokenUtils.extractToken(request, TokenConstant.ACCESS_TOKEN);

        // 2. jwtToken이 null이 아니고 토큰이 유효하다면
        if (jwtToken != null && jwtTokenUtils.validate(jwtToken)) {
            // 3. 해당 토큰을 바탕으로 사용자 정보를 SecurityContext에 등록
            setSecurityContext(jwtToken);
        } else if (jwtToken != null
                && jwtTokenUtils.isExpiredToken(jwtToken)
                // /api/auth/refresh 는 컨트롤러가 직접 처리하므로 필터에서 자동 재발급 제외
                && !REFRESH_ENDPOINT.equals(request.getRequestURI())) {
            // Access Token 만료 시 Refresh Token으로 자동 재발급 시도
            log.info("access token expired, trying auto refresh");
            String refreshToken = jwtTokenUtils.extractToken(request, TokenConstant.REFRESH_TOKEN);
            if (refreshToken != null) {
                jwtTokenUtils.tryAutoRefresh(refreshToken, response);
            } else {
                log.warn("refresh token not found for auto refresh");
            }
        } else {
            log.warn("jwt validation failed");
        }

        // 4. 다음 필터 호출
        filterChain.doFilter(request, response);
    }

    // SecurityContext에 등록하는 메서드
    private void setSecurityContext(String jwtToken) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        String username = jwtTokenUtils.parseClaims(jwtToken).getSubject();

        UserDetails userDetails = manager.loadUserByUsername(username);
        for (GrantedAuthority authority : userDetails.getAuthorities()) {
            log.info("authority: {}", authority.getAuthority());
        }

        AbstractAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userDetails,
                jwtToken,
                userDetails.getAuthorities()
            );

        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        log.info("set security context with jwt");
    }
}