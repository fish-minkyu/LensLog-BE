package com.example.LensLog.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// Facade Pattern
// : 복잡한 과정을 간소화시켜주는 것을 Facade Pattern이라고 부른다.
@Component
public class AuthenticationFacade {
  public Authentication getAuth() {
    return SecurityContextHolder.getContext().getAuthentication();
  }
}