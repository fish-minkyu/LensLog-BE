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

import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class GoodService {
    private final AuthenticationFacade auth;
    private final GoodRepository goodRepository;
    private final PhotoRepository photoRepository;

    @Transactional
    public boolean toggleLike(Long photoId) {
        // 인증 확인
        User user = auth.getAuth();

        // 사진 존재 여부 확인
        Photo photo = photoRepository.findById(photoId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "해당 사진이 존재하지 않습니다.")
            );

        // 사용자가 이미 해당 게시물에 좋아요를 눌렀는지 확인
        Optional<Good> existingGood = goodRepository.findByUserIdAndPhoto(user.getUserId(), photo);

        if (existingGood.isPresent()) {
            // 이미 좋아요를 눌렀다면 좋아요 취소(삭제)
            Good deleteGood = existingGood.get();
            goodRepository.delete(deleteGood);
            photo.removeGood(deleteGood);

            return false; // 좋아요 취소
        } else {
            // 좋아요를 누르지 않았다면 새로 추가(생성)
            // Good 엔티티 생성
            Good newGood = Good.builder()
                .userId(user.getUserId())
                .photo(photo)
                .build();

            // Good 엔티티 저장
            goodRepository.save(newGood);

            // Photo 엔티티의 Votes 컬렉션 동기화 (양방향 관계 유지)
            photo.addGood(newGood);

            return true; // 좋아요 추가
        }
    }
}
