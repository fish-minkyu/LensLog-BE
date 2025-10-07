package com.example.LensLog.photo.service;

import com.example.LensLog.photo.entity.Photo;
import com.example.LensLog.photo.entity.ThumbnailStatusEnum;
import com.example.LensLog.photo.repo.PhotoRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailService {
    private final PhotoRepository photoRepository;
    private final MinioService minioService;
    private final MinioClient minioClient;

    @Value("${minio.url}")
    private String minioApi;

    @Value("${minio.bucket.thumbnail.name}")
    private String THUMBNAIL_BUCKET;

    @Async
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2) // 지수 백오프: 1초, 2초, 4초
    )
    @Transactional
    public void generateThumbnail(Long photoId) throws Exception {
        log.info("Started thumbnail generation for Photo ID: {}", photoId);

        Photo photo = photoRepository.findById(photoId)
            .orElseThrow(() -> new IllegalArgumentException("The photo don't exist"));

        try {
            // 썸네일 생성 상태를 PROCESSING으로 변경한다.
            photo.setThumbnailStatus(ThumbnailStatusEnum.PROCESSING.name());

            // 썸네일용 파일 이름을 만든다.
            String storedFileName = photo.getStoredFileName();
            String thumbnailFileName = "thumbnail_" + storedFileName;
            try (
                InputStream originalPhotoStream = minioService.getFileInputStream(storedFileName);
                ByteArrayOutputStream thumbnailOutputStream = new ByteArrayOutputStream();
                ) {
                // 썸네일 생성
                Thumbnails.of(originalPhotoStream)
                    .width(150) // 최대 폭을 150px로 설정, 높이는 원본 비율에 맞춰 자동 조절
                    .toOutputStream(thumbnailOutputStream);

                byte[] thumbnailBytes = thumbnailOutputStream.toByteArray();

                // 썸네일을 MinIO에 저장한다.
                InputStream thumbnailInputStream = new ByteArrayInputStream(thumbnailBytes);
                saveThumbnailFile(thumbnailFileName, thumbnailInputStream, thumbnailBytes.length);

                // 썸네일 URL을 생성하고, DB 업데이트한다.
                String thumbnailUrl = minioApi + "/" + storedFileName;
                photo.setThumbnailUrl(thumbnailUrl);
                photo.setThumbnailStatus(ThumbnailStatusEnum.READY.name());
                photoRepository.save(photo);

                log.info("Thumbnail generation completed for Photo ID: {}", photoId);
            }
        } catch (Exception e) {
            log.error("Thumbnail generation failed for Photo ID: {} - {}", photoId, e.getMessage());
            photo.setThumbnailStatus(ThumbnailStatusEnum.FAILED.name());
            photoRepository.save(photo);
            throw e;
        }
    }

    // 썸네일 사진 업로드 메소드
    @Transactional
    public String saveThumbnailFile(String storedFileName, InputStream inputStream, long size) throws Exception {
        // 환경변수로 설정한 버킷이 존재한지 확인한다.
        boolean isExist = minioClient.bucketExists(
            BucketExistsArgs.builder()
                .bucket(THUMBNAIL_BUCKET)
                .build());

        // 존재하지 않다면, 새로 생성한다.
        if (!isExist) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                .bucket(THUMBNAIL_BUCKET)
                .build());
        }

        // MinIO에 저장한다.
        minioClient.putObject(PutObjectArgs.builder()
            .bucket(THUMBNAIL_BUCKET)
            .object(storedFileName)
            .stream(inputStream, size, -1)
            .contentType("image/jpeg")
            .build());

        // MinIO의 저장된 경로를 반환한다.
        return "/" + storedFileName;
    }
}
