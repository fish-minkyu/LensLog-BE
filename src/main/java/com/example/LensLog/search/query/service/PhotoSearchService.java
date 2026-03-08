package com.example.LensLog.search.query.service;

import com.example.LensLog.photo.dto.PhotoDto;
import com.example.LensLog.photo.entity.Photo;
import com.example.LensLog.photo.repo.PhotoRepository;
import com.example.LensLog.search.indexing.EmbeddingClient;
import com.example.LensLog.search.dto.SearchResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 검색용 오케스트레이션
// : 검색 결과를 최종 사용자 응답 형태로 만드는 서비스
// - text -> 임베딩
// - OpenSearch 검색
// - DB 재조회
// - PhotoDto + score 조합
@Service
@RequiredArgsConstructor
public class PhotoSearchService {
    private final EmbeddingClient embeddingClient;
    private final PhotoVectorSearcher vectorSearchService;
    private final PhotoRepository photoRepository;

    // DB 조회
    public List<PhotoDto> search(String query, int size) throws Exception {
        float[] qVec = embeddingClient.embedText(query);
        var scores = vectorSearchService.knnSearch(qVec, size);

        var ids = scores.stream().map(SearchResDto::photoId).toList();
        var photos = photoRepository.findAllById(ids);

        Map<Long, Photo> map = new HashMap<>();
        for (Photo photo : photos) {
            map.put(photo.getPhotoId(), photo);
        }

        List<PhotoDto> ordered = new ArrayList<>();
        for (var score : scores) {
            Photo photoEntity = map.get(score.photoId());
            if (photoEntity != null) ordered.add(
                PhotoDto.builder()
                    .photoId(photoEntity.getPhotoId())
                    .fileName(photoEntity.getFileName())
                    .shotDate(photoEntity.getShotDate())
                    .bucketFileUrl(photoEntity.getBucketFileUrl())
                    .thumbnailUrl(photoEntity.getThumbnailUrl())
                    .score(score.score())
                    .build()
            );
        }

        return ordered;
    }
}
