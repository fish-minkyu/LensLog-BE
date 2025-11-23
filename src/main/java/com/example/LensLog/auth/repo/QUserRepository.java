package com.example.LensLog.auth.repo;


import com.example.LensLog.auth.entity.User;

import java.util.Optional;

public interface QUserRepository {
    // provider별 username 조회
    boolean existsUserWithProvider(String provider, String username);

    // 사용자 username 찾기
    Optional<User> findUsername(String name, String email);
}
