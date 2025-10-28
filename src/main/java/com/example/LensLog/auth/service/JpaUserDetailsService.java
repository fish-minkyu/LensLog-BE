package com.example.LensLog.auth.service;

import com.example.LensLog.auth.CustomUserDetails;
import com.example.LensLog.auth.entity.User;
import com.example.LensLog.auth.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JpaUserDetailsService implements UserDetailsService {
    // 유저 정보를 가져오기 위한 레포지토리
    private final UserRepository userRepository;

    // 사용자 정보를 가지고 오는 메서드
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        User user = optionalUser.get();

        return CustomUserDetails.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .authority(user.getAuthority())
            .email(user.getEmail())
            .build();
    }
}
