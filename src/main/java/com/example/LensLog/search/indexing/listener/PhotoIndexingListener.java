package com.example.LensLog.search.indexing.listener;

import com.example.LensLog.photo.entity.Photo;
import com.example.LensLog.photo.entity.StatusEnum;
import com.example.LensLog.photo.event.PhotoThumbnailReadyEvent;
import com.example.LensLog.photo.repo.PhotoRepository;
import com.example.LensLog.photo.service.MinioService;
import com.example.LensLog.search.indexing.EmbeddingClient;
import com.example.LensLog.search.dto.SearchReqDto;
import com.example.LensLog.search.indexing.error.AiTaggingException;
import com.example.LensLog.search.indexing.error.IndexingException;
import com.example.LensLog.search.indexing.service.AITaggingService;
import com.example.LensLog.search.indexing.service.PhotoIndexWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// 이미지 임베딩 생성 후 색인
@Slf4j
@Component
@RequiredArgsConstructor
public class PhotoIndexingListener {
    private final PhotoRepository photoRepository;
    private final MinioService minioService;
    private final EmbeddingClient embeddingClient;
    private final AITaggingService aiTaggingService;
    private final PhotoIndexWriter searchIndexService;

    @Value("${minio.bucket.photo.name}")
    private String photoBucket;

    @Value("${minio.bucket.thumbnail.name}")
    private String thumbnailBucket;

    @EventListener()
    public void handleAfterThumbnailUpload(PhotoThumbnailReadyEvent event) throws Exception {
        log.info("PhotoThumbnailReadyEvent 시작");
        Long photoId = event.photoId();

        Photo photo = photoRepository.findById(photoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND
                , "해당 사진이 없습니다: " + photoId
            ));

        try {
            // C. OPenAI 태깅 작업 시작
            // OpenAI 태깅용 이미지 bytes
            byte[] imgBytes = loadImageBytes(photo);

            // OpenAI에 태깅 요청
            var tagging = aiTaggingService.generateTagsAndCaption(photoId, imgBytes);
            String caption = tagging.getCaption();
            List<String> tags = tagging.getTags();

            // D. CLIP 이미지 임베딩 작업 시작
            // embedding 서버가 접근 가능한 URL로 넣기
            String imageUrlForEmbedding = toDockerMinioUrl(photo.getThumbnailUrl());

            // 이미지 임베딩(512) 생성
            float[] vecArr = embeddingClient.embedImageUrl(photoId, imageUrlForEmbedding);

            SearchReqDto doc = SearchReqDto.builder()
                .photoId(photo.getPhotoId())
                .imageUrl(imageUrlForEmbedding)
                .caption(caption == null ? "" : caption)
                .tags(tags == null ? List.of() : tags)
                .location(photo.getLocation())
                .categoryId(photo.getCategory() == null ? null : photo.getCategory().getCategoryId())
                .categoryName(photo.getCategory() == null ? null : photo.getCategory().getCategoryName())
                .shotDate(photo.getShotDate())
                .mmVector(vecArr)
                .build();

            // E. OpenSearch doc 저장
            searchIndexService.index(doc);

            // photo 상태값 저장
            photo.setAiTagStatus(StatusEnum.READY.name());
            photo.setSearchIndexStatus(StatusEnum.READY.name());

            log.info("indexed photoId = {}", photo.getPhotoId());
            log.info("PhotoThumbnailReadyEvent 종료");
        } catch (AiTaggingException e) {
            log.error("AI tagging failed photoId = {}, msg = {}", photoId, e.getMessage(), e);
            photo.setAiTagStatus(StatusEnum.FAILED.name());
            photo.setSearchIndexStatus(StatusEnum.PENDING.name());
            throw e;
        } catch (IndexingException e) {
            log.error("Saving OpenSearch failed photoId = {}, msg = {}", photoId, e.getMessage(), e);
            photo.setSearchIndexStatus(StatusEnum.FAILED.name());
            throw e;
        } catch (Exception e) {
            log.error("indexing failed photoId = {}, msg = {}", photoId, e.getMessage(), e);
            throw e;
        } finally {
            photoRepository.save(photo);
        }
    }

    private String toDockerMinioUrl(String url) {
        if (url == null) return null;

        // host가 localhost:9000이면 도커 내부에서는 minio:9000으로 변경
        return url.replace("http://localhost:9000", "http://minio:9000");
    }

    private byte[] loadImageBytes(Photo photo) throws Exception {
        // 썸네일 파일명 추출 시도
        if (photo.getThumbnailUrl() == null || photo.getThumbnailUrl().isBlank()) {
            throw new IllegalArgumentException("해당 사진은 null입니다: " + photo.getPhotoId());
        }

        String thumbnailObjectName = extractObjectName(photo.getThumbnailUrl());

        // thumbnailBucket + objectName으로 bytes 얻기
        return minioService.getObjectBytes(thumbnailBucket, thumbnailObjectName);
    }

    private String extractObjectName(String url) {
        // 예: http://localhost:9000/thumbnail/DSCF2032.webp -> DSCF2032.webp
        int idx = url.lastIndexOf('/');
        return (idx >= 0) ? url.substring(idx + 1) : url;
    }
}
