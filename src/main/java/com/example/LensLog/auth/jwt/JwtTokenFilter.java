package com.example.LensLog.auth.jwt;

import com.example.LensLog.constant.TokenConstant;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;


// 인증 관련 객체는 Bean 객체로 등록하지 않는다. (Bean 객체로 등록하면 자동으로 Filter로 등록이 된다.)
// Why? WebSecurityConfig에서 수동으로 등록을 해줘야 한다.
// 근데 Bean으로 등록을 해주면 Spring Container가 한번 더 등록하고 Security에서 등록을 하게되면 2번 등록하게 된다.
@Slf4j
public class JwtTokenFilter extends OncePerRequestFilter {
    private final JwtTokenUtils jwtTokenUtils;
    // 사용자 정보를 찾기위한 UserDetailsService 또는 UserDetailsManager
    private final UserDetailsService manager;

    public JwtTokenFilter(
        JwtTokenUtils jwtTokenUtils,
        UserDetailsService manager
    ) {
        this.jwtTokenUtils = jwtTokenUtils;
        this.manager = manager;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        log.debug("try jwt filter");

        // 1. auth_token으로 명시된 Access Token 가지고 오기
        String jwtToken = extractAccessToken(request);

        // 2. jwtToken이 null이 아니고 토큰이 유효하다면
        if (jwtToken != null && jwtTokenUtils.validate(jwtToken)) {
            // 3. 해당 토큰을 바탕으로 사용자 정보를 SecurityContext에 등록
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            // 사용자 정보 회수
            String username = jwtTokenUtils
                .parseClaims(jwtToken)
                .getSubject();

            // getAuthorities 메소드의 결과에 따라서 사용자의 권한을 확인
            UserDetails userDetails = manager.loadUserByUsername(username);
            for(GrantedAuthority authority : userDetails.getAuthorities()) {
                log.info("authority: {}", authority.getAuthority());
            }

            // 4. 인증 정보 생성
            AbstractAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    jwtToken,
                    userDetails.getAuthorities() // 인증하고 나서 사용자 권한이 들어가게 된다.
                );

            // 5. 인증 정보 등록
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            log.info("set security context with jwt");
        } else {
            // 사용자가 잘못된 jwt를 요청할 수 있으므로 기록해둔다.
            log.warn("jwt validation failed");
        }

        // 6. 다음 필터 호출
        // doFilter를 호출하지 않으면 Controller까지 요청이 도달하지 못한다.
        // 인증 성공 유무에 상관없이 다음 필터를 호출해줘야 한다.
        filterChain.doFilter(request, response);
    }

    private String extractAccessToken(HttpServletRequest request) {
        Cookie[] cookies = Optional.ofNullable(request.getCookies())
            .orElse(new Cookie[0]);

        for (Cookie cookie : cookies) {
            if (TokenConstant.ACCESS_TOKEN.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}