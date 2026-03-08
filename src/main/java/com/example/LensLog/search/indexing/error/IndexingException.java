package com.example.LensLog.search.indexing.error;

import lombok.Getter;

@Getter
public class IndexingException extends RuntimeException{
    private final Long photoId;
    private final Enum reason;

    public enum IndexingErrorReason {
        OPENSEARCH_DOC_PHOTOID_ISNULL,
        OPENSEARCH_DOING_ERROR
    }

    public IndexingException(IndexingErrorReason reason, String message) {
        super(message);
        this.photoId = null;
        this.reason = reason;
    }

    public IndexingException(Long photoId, IndexingErrorReason reason, String message, Throwable cause) {
        super(message);
        this.photoId = photoId;
        this.reason = reason;
    }
}
