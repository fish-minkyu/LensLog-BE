package com.example.LensLog.search.indexing.error;

import lombok.Getter;

@Getter
public class AiTaggingException extends RuntimeException {
    private final Long photoId;
    private final Enum reason;

    public enum AiErrorReason {
        OPENAI_HTTP_ERROR,
        OPENAI_NON_2XX,
        OPENAI_EMPTY_BODY,
        OPENAI_PARSE_ERROR
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
