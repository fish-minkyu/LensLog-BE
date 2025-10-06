package com.example.LensLog.common;

import com.example.LensLog.auth.CustomUserDetails;
import com.example.LensLog.auth.entity.User;
import com.example.LensLog.auth.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
        CustomUserDetails customUserDetails
            = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return userRepository.findByUsername(customUserDetails.getUsername())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}