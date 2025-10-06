package com.example.LensLog.like.service;

import com.example.LensLog.auth.entity.User;
import com.example.LensLog.common.AuthenticationFacade;
import com.example.LensLog.like.entity.Like;
import com.example.LensLog.like.repo.LikeRepository;
import com.example.LensLog.photo.entity.Photo;
import com.example.LensLog.photo.repo.PhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {
    private final AuthenticationFacade auth;
    private final LikeRepository likeRepository;
    private final PhotoRepository photoRepository;

    // 좋아요 생성
    @Transactional
    public void saveLike(Long photoId) {
        // 인증 확인
        User user = auth.getAuth();

        Photo photo = photoRepository.findById(photoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // 중복 좋아요 여부 확인
        if (likeRepository.existsByUserIdAndPhoto(user.getUserId(), photo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already liked this photo");
        }

        // Like 엔티티 생성
        Like newLike = Like.builder()
            .userId(user.getUserId())
            .photo(photo)
            .build();

        // Like 엔티티 저장
        likeRepository.save(newLike);

        // Photo 엔티티의 likes 컬렉션 동기화 (양방향 관계 유지)
        photo.addLike(newLike);
    }

    // 좋아요 삭제
    @Transactional
    public void deleteLike(Long photoId) {
        // 인증 확인
        User user = auth.getAuth();

        Photo photo = photoRepository.findById(photoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // 좋아요 여부 확인
        if (!likeRepository.existsByUserIdAndPhoto(user.getUserId(), photo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User didn't like this photo");
        }

        // Like 엔티티 찾기
        Like deleteLike = likeRepository.findByUserIdAndPhoto(user.getUserId(), photo)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User didn't like this photo"));

        // Like 엔티티 삭제
        likeRepository.delete(deleteLike);

        // Photo 엔티티의 likes 컬렉션 동기화 (양방향 관계 유지)
        photo.removeLike(deleteLike);
    }
}
