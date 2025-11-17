package com.example.LensLog.good.service;

import com.example.LensLog.auth.entity.User;
import com.example.LensLog.common.AuthenticationFacade;
import com.example.LensLog.good.entity.Good;
import com.example.LensLog.good.repo.GoodRepository;
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
public class GoodService {
    private final AuthenticationFacade auth;
    private final GoodRepository goodRepository;
    private final PhotoRepository photoRepository;

    // 좋아요 생성
    @Transactional
    public void saveGood(Long photoId) {
        // 인증 확인
        User user = auth.getAuth();

        Photo photo = photoRepository.findById(photoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // 중복 좋아요 여부 확인
        if (goodRepository.existsByUserIdAndPhoto(user.getUserId(), photo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already liked this photo");
        }

        // Good 엔티티 생성
        Good newGood = Good.builder()
            .userId(user.getUserId())
            .photo(photo)
            .build();

        // Good 엔티티 저장
        goodRepository.save(newGood);

        // Photo 엔티티의 Votes 컬렉션 동기화 (양방향 관계 유지)
        photo.addGood(newGood);
    }

    // 좋아요 삭제
    @Transactional
    public void deleteGood(Long photoId) {
        // 인증 확인
        User user = auth.getAuth();

        Photo photo = photoRepository.findById(photoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // 좋아요 여부 확인
        if (!goodRepository.existsByUserIdAndPhoto(user.getUserId(), photo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User didn't like this photo");
        }

        // Good 엔티티 찾기
        Good deleteGood = goodRepository.findByUserIdAndPhoto(user.getUserId(), photo)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User didn't like this photo"));

        // Good 엔티티 삭제
        goodRepository.delete(deleteGood);

        // Photo 엔티티의 Goods 컬렉션 동기화 (양방향 관계 유지)
        photo.removeGood(deleteGood);
    }
}
