package com.example.LensLog.auth.repo;


public interface QUserRepository {
    // provider별 username 조회
    boolean existsUserWithProvider(String provider, String username);
}
