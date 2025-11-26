package com.example.LensLog.common;

import com.example.LensLog.auth.CustomUserDetails;
import com.example.LensLog.auth.entity.User;
import com.example.LensLog.auth.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

// Facade Pattern
// : 복잡한 과정을 간소화시켜주는 것을 Facade Pattern이라고 부른다.
@Component
@RequiredArgsConstructor
public class AuthenticationFacade {
    private final UserRepository userRepository;
    private final String ANONYMOUS_USER = "anonymousUser";

    // 현재 로그인된 사용자를 반환한다.
    // 인증되지 않았거나 익명 사용자이면 예외를 발생시킨다.
    public User getAuth() {
        Authentication authentication
            = SecurityContextHolder.getContext().getAuthentication();

        // 인증 여부 확인 및 익명 사용자 방지
        if (authentication == null
            || !authentication.isAuthenticated()
            || ANONYMOUS_USER.equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "인증된 사용자 정보가 없습니다.");
        }

        return findAuthenticatedUser(authentication);
    }

    // 현재 로그인 된 사용자 객체를 Optional로 반환
    // 만약 로그인하지 않았거나 익명 사용자이면 Optional.empty를 반환한다.
    public Optional<User> getOptionalAuth() {
        Authentication authentication
            = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
        || !authentication.isAuthenticated()
        || ANONYMOUS_USER.equals(authentication.getPrincipal())) {
            return Optional.empty();
        }

        return Optional.ofNullable(findAuthenticatedUser(authentication));
    }

    private User findAuthenticatedUser(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails customUserDetails)) {
            throw new IllegalStateException("인증 주체의 타입이 CustomUserDetails가 아닙니다.");
        }

        return userRepository.findByUsername(customUserDetails.getUsername())
            .orElseThrow(() -> new IllegalStateException("해당 ID와 일치하는 사용자가 없습니다."));
    }
}