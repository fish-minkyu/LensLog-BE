package com.example.LensLog.photo.repo;

import com.example.LensLog.photo.entity.Photo;

import java.util.List;

public interface QPhotoRepository {
    // Cursor 페이지네이션
    List<Photo> searchListCursor(Long categoryId, Long lastPhotoId, int pageSize);
}
