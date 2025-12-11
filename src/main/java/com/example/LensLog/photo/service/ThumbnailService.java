package com.example.LensLog.photo.service;

import com.example.LensLog.photo.entity.Photo;
import com.example.LensLog.photo.entity.ThumbnailStatusEnum;
import com.example.LensLog.photo.event.PhotoUploadEvent;
import com.example.LensLog.photo.repo.PhotoRepository;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailService {
    private final PhotoRepository photoRepository;
    private final MinioService minioService;
    private final MinioClient minioClient;

    @Value("${minio.public.endpoint}")
    private String minioPublicEndpoint;

    @Value("${minio.bucket.thumbnail.name}")
    private String thumbnailBucket;

    @Async
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2) // 지수 백오프: 1초, 2초, 4초
    )
    // uploadPhoto 트랜잭션이 성공적으로 커밋된 후 호출
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePhotoUploadEvent(PhotoUploadEvent event) throws Exception {
        generateThumbnail(event.getPhotoId());
    }

    @Transactional
    public void generateThumbnail(Long photoId) throws Exception {
        log.info("Started thumbnail generation for Photo ID: {}", photoId);

        Photo photo = photoRepository.findById(photoId)
            .orElseThrow(() -> new IllegalArgumentException("해당 사진은 존재하지 않습니다."));

        try {
            // 썸네일 생성 상태를 PROCESSING으로 변경한다.
            photo.setThumbnailStatus(ThumbnailStatusEnum.PROCESSING.name());

            // 썸네일용 파일 이름을 만든다.
            String fileName = photo.getFileName();
            String baseName = fileName;

            int dotIndex = fileName.lastIndexOf(".");
            if (dotIndex != -1) {
                baseName = fileName.substring(0, dotIndex);
            }

            String thumbnailFileName = baseName + ".webp";

            try (InputStream originalPhotoStream = minioService.getFileInputStream(fileName);) {
                // scrimage로 이미지 로드
                ImmutableImage image = ImmutableImage.loader().fromStream(originalPhotoStream);
                // 가로 224px 기준으로 리사이즈 (세로는 비율 유지)
                ImmutableImage resized = image.scaleToWidth(224);
                // WebP 인코딩 (품질 0~100 사이)
                WebpWriter writer = WebpWriter.DEFAULT.withQ(100);

                byte[] thumbnailBytes = resized.bytes(writer);

                // 썸네일을 MinIO에 저장한다.
                try (InputStream thumbnailInputStream = new ByteArrayInputStream(thumbnailBytes)) {
                    saveThumbnailFile(thumbnailFileName, thumbnailInputStream, thumbnailBytes.length);
                }

                // 썸네일 URL을 생성하고, DB 업데이트
                String thumbnailUrl = minioPublicEndpoint + "/" + thumbnailBucket + "/" + thumbnailFileName;
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
                .bucket(thumbnailBucket)
                .build());

        // 존재하지 않다면, 새로 생성한다.
        if (!isExist) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                .bucket(thumbnailBucket)
                .build());
        }

        // MinIO에 저장한다.
        minioClient.putObject(PutObjectArgs.builder()
            .bucket(thumbnailBucket)
            .object(storedFileName)
            .stream(inputStream, size, -1)
            .contentType("image/webp")
            .build());

        // MinIO의 저장된 경로를 반환한다.
        return "/" + storedFileName;
    }
}
