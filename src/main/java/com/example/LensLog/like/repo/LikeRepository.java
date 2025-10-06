package com.example.LensLog.like.repo;

import com.example.LensLog.like.entity.Like;
import com.example.LensLog.photo.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {
    // 좋아요 존재 유무 확인
    boolean existsByUserIdAndPhoto(Long userId, Photo photo);

    // 좋아요 찾기
    Optional<Like> findByUserIdAndPhoto(Long userId, Photo photo);
}
