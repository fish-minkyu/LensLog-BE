package com.example.LensLog.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {
    private Long userId;
    private String email;
    private String password;
    private String name;
    private LocalDate birthDate;
    private String authority;

    public String getEmail() {
        return this.email;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        throw new NotImplementedException("There is no username");
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(this.authority));
    }

    @Override
    public boolean isAccountNonExpired() {
        throw new NotImplementedException();
    }

    @Override
    public boolean isAccountNonLocked() {
        throw new NotImplementedException();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        throw new NotImplementedException();
    }

    @Override
    public boolean isEnabled() {
        throw new NotImplementedException();
    }
}
