package com.example.LensLog.photo.service;

import com.example.LensLog.common.HashGenerator;
import com.example.LensLog.photo.dto.PhotoCursorPageDto;
import com.example.LensLog.photo.entity.Photo;
import com.example.LensLog.photo.repo.PhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoService {
    private final PhotoRepository photoRepository;
    private final MinioService minioService;

    @Value("${minio.url}")
    private String minioApi;

    // 사진 단일 업로드
    @Transactional
    public void uploadPhoto(MultipartFile multipartFile) throws Exception {
        // 1. 사진 파일의 해시 값 계산
        String fileHash = HashGenerator.calculateSha256Hash(multipartFile.getInputStream());

        // 2. 해시 값으로 DB에서 기존 사진 조회(FOR UPDATE 락 적용)
        Optional<Photo> existedPhoto = photoRepository.findForUpdateByHashValue(fileHash);

        // 3. 사진 존재한다면, 예외처리를 한다.
        if (existedPhoto.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        // 4. 사진이 중복되지 않는다면
        String storedFileName = UUID.randomUUID().toString() + "_" + multipartFile.getOriginalFilename();
        // MinIO에 해당 파일을 저장한다.
        minioService.savePhotoFile(storedFileName, multipartFile);

        // 5. DB에 Photo의 메타 데이터를 저장한다.
        String minioUrl = minioApi + "/" + storedFileName;

        Photo newPhoto = Photo.builder()
            .fileName(multipartFile.getOriginalFilename())
            .storedFileName(storedFileName)
            .bucketFileUrl(minioUrl)
            .hashValue(fileHash)
            .views(0L)
            .downloads(0L)
            .build();

        photoRepository.save(newPhoto);
    }

    // 사진 목록 조회(Cursor 방식)
    public PhotoCursorPageDto getListPhotoCursor() {
        log.info("...: DB에서 데이터 조회 중...");

        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
    }

    // 사진 단일 조회
    // 트랜잭션 안에서 엔티티 변경 시 자동 DB 반영(더티 체킹)
    @Transactional
    public Photo getPhoto(Long photoId) {
        Photo photo = photoRepository.findById(photoId)
            .orElseThrow(() -> new IllegalArgumentException("Photo don't exists"));

        // 조회수 증가
        photo.increaseViews();

        return photo;
    }

    // 사진 다운로드
    @Transactional
    public ResponseEntity<InputStreamResource> downloadPhoto(Long photoId) throws Exception {
        Photo photo = photoRepository.findById(photoId)
            .orElseThrow(() -> new IllegalArgumentException("Photo don't exists"));

        try {
            // 다운로드 횟수 증가
            // @Transactional에 의헤 photo 엔티티의 변경사항은 DB에 자동으로 반영된다.
            // => 영속성 컨텍스트와 더티 체킹 때문
            photo.increaseDownloads();

            // MinIO에서 사진 다운로드 트리거 호출
            InputStream inputStream = minioService.downloadPhoto(photo.getStoredFileName());

            // 파일 이름 인코딩: 한국 파일 이름 등에 대비
            String encoderFileName = URLEncoder.encode(photo.getFileName(), StandardCharsets.UTF_8.toString())
                .replaceAll("\\+", "%20");

            // HTTP 헤더 설정: 파일 다운로드를 유도하고, 파일 이름을 저장한다.
            HttpHeaders headers = new HttpHeaders();
            headers.add
                (HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + encoderFileName + "\"");

            // 컨텐츠 타입은 일반적으로 "application/octet-stream"으로 설정하여
            // 브라우저가 다운로드하도록 유도한다.
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

            return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM) // 실제 응답의 컨텐츠 타입
                .body(new InputStreamResource(inputStream)); // InputStream을 래핑하여 반환
        } catch (IllegalArgumentException e) {
            log.error("다운로드하려는 사진을 찾을 수 없습니다.");
            return ResponseEntity.notFound().build(); // 404 Not Found
        } catch (Exception e) {
            log.error("파일 다운로드 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    // 사진 삭제
    @Transactional
    public void deletePhoto(Long photoId) throws Exception {
        Photo photo = photoRepository.findById(photoId)
            .orElseThrow(() -> new IllegalArgumentException("Photo don't exists"));

        try {
            // DB에서 삭제
            photoRepository.delete(photo);

            // MinIO에서 삭제
            minioService.deletePhotoFile(photo);
        } catch (Exception e) {
            log.error("deletePhoto error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
