package com.example.LensLog.auth.repo;


import com.example.LensLog.auth.entity.User;

import java.util.List;
import java.util.Optional;

public interface QUserRepository {
    // provider별 username 조회
    boolean existsUserWithProvider(String provider, String username);

    // 사용자 username 찾기
    Optional<List<User>> findNameWithEmail(String name, String email);

    // 사용자 계정 존재 유무 확인
    boolean existsUsernameWithEmail(String username, String email);

    // 오직 하나의 사용자 조회
    Optional<User> findOnlyOneUser(String provider, String username, String email);
}
