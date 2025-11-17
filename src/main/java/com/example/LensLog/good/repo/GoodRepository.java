package com.example.LensLog.good.repo;


import com.example.LensLog.photo.entity.Photo;
import com.example.LensLog.good.entity.Good;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoodRepository extends JpaRepository<Good, Long> {
    // 좋아요 존재 유무 확인
    boolean existsByUserIdAndPhoto(Long userId, Photo photo);

    // 좋아요 찾기
    Optional<Good> findByUserIdAndPhoto(Long userId, Photo photo);
}
