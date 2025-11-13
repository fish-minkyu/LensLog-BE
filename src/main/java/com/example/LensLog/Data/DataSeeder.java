package com.example.LensLog.Data;

import com.example.LensLog.auth.entity.User;
import com.example.LensLog.auth.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private static final String ADMIN_EMAIL = "e951219@naver.com";

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
            User admin = User.builder()
                .email(ADMIN_EMAIL)
                .password("lensLog123!")
                .name("관리자")
                .birthDate(LocalDate.now())
                .isVerified(true)
                .authority("ROLE_ADMIN")
                .build();

            userRepository.save(admin);
            System.out.println("DataSeeder activation success!!");
        } catch (Exception e) {
            log.error("DataSeeder activation has a error: {}", e.getMessage());
            throw e;
        }
    }
}
