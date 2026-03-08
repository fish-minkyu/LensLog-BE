package com.example.LensLog.search.query.service;

import com.example.LensLog.search.dto.SearchResDto;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;

// OpenSearch kNN 검색
// : 검색 전담 서비스, 검색 시 query text 임베딩을 받아서 OpenSearch kNN 검색 수행
// - 검색 결과의 photoId 및 score 반환
@Service
@RequiredArgsConstructor
public class PhotoVectorSearcher {
    private final OpenSearchClient openSearchClient;

    @Value("${lenslog.opensearch.indexVersioned}")
    private String indexName;

    public List<SearchResDto> knnSearch(float[] queryVec, int size) throws Exception {
        int kCandidates = Math.max(size * 10, 200);

        var res = openSearchClient.search(s -> s
                .index(indexName)
                .size(size) // 최종 반환 개수
                .query(q -> q
                    .knn(knn -> knn
                        .field("mmVector")
                        .k(kCandidates) // 후보를 넓게
                        .vector(queryVec)
                    )
                ),
            Object.class
        );

        List<SearchResDto> out = new ArrayList<>();
        res.hits().hits().forEach(h -> {
            // _id가 photoId로 들어가 있음
            Long id = Long.parseLong(h.id());
            double score = (h.score() == null ? 0.0 : h.score());
            out.add(new SearchResDto(id, score));
        });

        return out;
    }
}