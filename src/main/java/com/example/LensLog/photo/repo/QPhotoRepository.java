package com.example.LensLog.photo.repo;

import com.example.LensLog.photo.entity.Photo;

import java.util.List;

public interface QPhotoRepository {
    // Cursor 페이지네이션(카테고리별)
    List<Photo> searchListCursor(Long categoryId, Long lastPhotoId, int pageSize);

    // 유저별 좋아요를 누른 사진ID 리스트 조회
    List<Long> getListGoodPhotoIdByUserId(Long userId);

    // 사용자별 좋아요 누른 사진 리스트 조회
    List<Photo> getListPhotoCursorByLike(List<Long> photoGoodList);
}
