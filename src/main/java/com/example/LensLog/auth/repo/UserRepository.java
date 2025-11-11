package com.example.LensLog.auth.repo;

import com.example.LensLog.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 사용자 메일로 사용자 찾기
    Optional<User> findByEmail(String email);

    // 사용자 email로 사용자가 있는지 유무 확인
    boolean existsByEmail(String email);
}
