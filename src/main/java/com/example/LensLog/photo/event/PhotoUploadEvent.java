// src/main/java/com/example/LensLog/photo/event/PhotoUploadEvent.java (예시 경로)
package com.example.LensLog.photo.event; // 패키지 경로를 적절히 변경하세요.

public class PhotoUploadEvent {
    private final Long photoId;

    public PhotoUploadEvent(Long photoId) {
        this.photoId = photoId;
    }

    public Long getPhotoId() {
        return photoId;
    }
}