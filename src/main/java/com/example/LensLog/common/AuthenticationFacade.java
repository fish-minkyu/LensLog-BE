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

// Facade Pattern
// : 복잡한 과정을 간소화시켜주는 것을 Facade Pattern이라고 부른다.
@Component
@RequiredArgsConstructor
public class AuthenticationFacade {
    private final UserRepository userRepository;

    public User getAuth() {
        Authentication authentication
            = SecurityContextHolder.getContext().getAuthentication();

        // 인증 여부 확인 및 익명 사용자 방지
        String ANONYMOUS_USER = "anonymousUser";
        if (authentication == null
            || !authentication.isAuthenticated()
            || ANONYMOUS_USER.equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "인증된 사용자 정보가 없습니다.");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails customUserDetails)) {
            throw new IllegalStateException("인증 주체의 타입이 CustomUserDetails가 아닙니다.");
        }

        return userRepository.findByUsername(customUserDetails.getUsername())
            .orElseThrow(() -> new IllegalStateException("There is no user."));
    }
}