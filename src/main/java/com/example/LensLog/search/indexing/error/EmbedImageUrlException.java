package com.example.LensLog.search.indexing.error;

import lombok.Getter;

@Getter
public class EmbedImageUrlException extends RuntimeException{
    private final Long photoId;

    public EmbedImageUrlException(Long photoId, String message, Throwable cause) {
        super(message);
        this.photoId = photoId;
    }
}
