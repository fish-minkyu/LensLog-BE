package com.example.LensLog.search.indexing.service;

import com.example.LensLog.search.dto.SearchReqDto;
import com.example.LensLog.search.indexing.error.IndexingException;
import jakarta.persistence.Index;
import lombok.RequiredArgsConstructor;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// D. OpenSearch에 document 저장
// : 색인 전담 서비스
// - OpenSearch에 문서 저장
// - _id = photoId로 색인
@Service
@RequiredArgsConstructor
public class PhotoIndexWriter {
    private final OpenSearchClient openSearchClient;

    @Value("${lenslog.opensearch.indexVersioned}")
    private String indexName;

    // 벡터 포함해서 저장하는 메서드
    public void index(SearchReqDto doc) {
        try {
            if (doc.getPhotoId() == null) {
                throw new IndexingException(
                    IndexingException.IndexingErrorReason.OPENSEARCH_DOC_PHOTOID_ISNULL,
                    "photoId is null"
                );
            }

            openSearchClient.index(i -> i
                .index(indexName)
                .id(String.valueOf(doc.getPhotoId()))
                .document(doc)
            );
        } catch (Exception e) {
            throw new IndexingException(
                doc.getPhotoId(),
                IndexingException.IndexingErrorReason.OPENSEARCH_DOING_ERROR,
                "Saving OpenSearch doc is error",
                e
            );
        }
    }
}
