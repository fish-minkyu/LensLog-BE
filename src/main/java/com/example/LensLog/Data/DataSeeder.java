package com.example.LensLog.Data;

import com.example.LensLog.auth.entity.User;
import com.example.LensLog.auth.repo.UserRepository;
import com.example.LensLog.category.repo.CategoryRepository;
import com.example.LensLog.constant.LoginTypeConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;



@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private static final String ADMIN_EMAIL = "e951219@naver.com";
    private static final String PASSWORD = "lensLog123!";

    @Transactional
    @Override
    public void run(String... args) throws Exception {
        System.out.println("DataSeeder activation start!!");
        // 관리자 계정이 이미 있다면 실행하지 않는다.
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            System.out.println("The admin is already exist.");
            return;
        }

        try {
            makeAdmin();
            System.out.println("DataSeeder activation success!!");
        } catch (Exception e) {
            log.error("DataSeeder activation has a error: {}", e.getMessage());
            throw e;
        }
    }

    public void makeAdmin() {
        User admin = User.builder()
            .username("e951219")
            .password(passwordEncoder.encode(PASSWORD))
            .name("관리자")
            .email(ADMIN_EMAIL)
            .isVerified(true)
            .provider(LoginTypeConstant.LOCAL)
            .authority("ROLE_ADMIN")
            .build();

        userRepository.save(admin);
    }
}
