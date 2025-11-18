package com.example.LensLog.photo.event;

public class PhotoUploadEvent {
    private final Long photoId;

    public PhotoUploadEvent(Long photoId) {
        this.photoId = photoId;
    }

    public Long getPhotoId() {
        return photoId;
    }
}