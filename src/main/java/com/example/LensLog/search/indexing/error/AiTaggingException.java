package com.example.LensLog.search.indexing.error;

import lombok.Getter;

@Getter
public class AiTaggingException extends RuntimeException {
    private final Long photoId;
    private final Enum reason;

    public enum AiErrorReason {
        HTTP_ERROR,
        NON_2XX,
        EMPTY_BODY,
        PARSE_ERROR
    }

    public AiTaggingException(Long photoId, AiErrorReason reason, String message) {
        super(message);
        this.photoId = photoId;
        this.reason = reason;
    }

    public AiTaggingException(Long photoId, AiErrorReason reason, String message, Throwable cause) {
        super(message);
        this.photoId = photoId;
        this.reason = reason;
    }
}
