package com.example.LensLog.photo.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.example.LensLog.auth.entity.User;
import com.example.LensLog.category.entity.Category;
import com.example.LensLog.category.repo.CategoryRepository;
import com.example.LensLog.common.AuthenticationFacade;
import com.example.LensLog.common.HashGenerator;
import com.example.LensLog.good.entity.Good;
import com.example.LensLog.good.repo.GoodRepository;
import com.example.LensLog.photo.dto.PhotoCursorPageDto;
import com.example.LensLog.photo.dto.PhotoDto;
import com.example.LensLog.photo.entity.Photo;
import com.example.LensLog.photo.event.PhotoUploadEvent;
import com.example.LensLog.photo.repo.PhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.drew.metadata.Metadata;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoService {
    private final PhotoRepository photoRepository;
    private final MinioService minioService;
    private final ApplicationEventPublisher eventPublisher; // 이벤트 퍼블리셔
    private final AuthenticationFacade auth;
    private final GoodRepository goodRepository;
    private final CategoryRepository categoryRepository;

    @Value("${minio.public.endpoint}")
    private String minioPublicEndpoint;

    @Value("${minio.cdn.endpoint}")
    private String cdnEndpoint;

    @Value("${minio.bucket.photo.name}")
    private String photoBucket;

    @Value("${minio.bucket.thumbnail.name}")
    private String thumbnailBucket;

    // 사진 단일 업로드
    @Transactional
    public void uploadPhoto(MultipartFile multipartFile, PhotoDto dto) throws Exception {
        // 1. 사진 파일의 해시 값 계산
        String fileHash = HashGenerator.calculateSha256Hash(multipartFile.getInputStream());

        // 2. 해시 값으로 DB에서 기존 사진 조회(FOR UPDATE 락 적용)
        Optional<Photo> existedPhoto = photoRepository.findForUpdateByHashValue(fileHash);

        // 3. 사진 존재한다면, 예외처리를 한다.
        if (existedPhoto.isPresent()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "해당 사진은 이미 존재합니다."
            );
        }

        // 4. categoryId가 있다면 해당 카테고리를 찾는다.
        Long categoryId = dto.getCategoryId();
        Category targetCategory = null;
        if (categoryId != null) {
            targetCategory = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalStateException(
                    "해당하는 카테고리 ID가 없습니다: " + dto.getCategoryId()
                ));
        }

        // 5. 사진이 중복되지 않는다면 MinIO에 해당 파일을 저장한다.
        String fileName = multipartFile.getOriginalFilename();
        minioService.savePhotoFile(fileName, multipartFile);

        // 6. DB에 Photo의 메타 데이터를 저장한다.
        String minioUrl = minioPublicEndpoint + "/"  + photoBucket + "/" + fileName;
        LocalDate shotDate = getShotDate(multipartFile);

        Photo newPhoto = Photo.builder()
            .fileName(fileName)
            .location(dto.getLocation())
            .shotDate(shotDate)
            .bucketFileUrl(minioUrl)
            .hashValue(fileHash)
            .category(targetCategory)
            .views(0L)
            .downloads(0L)
            .build();

        photoRepository.save(newPhoto);

        // 화질 이슈로 인한 미사용
        // 7. 트랜잭션이 성공적으로 커밋된 후에 썸네일 생성을 위한 이벤트 발행
        // @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) 에 의해 처리
        eventPublisher.publishEvent(new PhotoUploadEvent(newPhoto.getPhotoId()));
    }

    // 사진 목록 조회(Cursor 방식)
    @Cacheable(value = "photoCursorPages", key = "#categoryId + '_' + #lastPhotoId + '_' + #pageSize")
    public PhotoCursorPageDto getListPhotoCursor(Long categoryId, Long lastPhotoId, int pageSize) {
        log.info("...: DB에서 데이터 조회 중...");

        // 다음 페이지 존재 여부 확인을 위해 pageSize보다 1개 더 조회
        List<Photo> photos = photoRepository.searchListCursor(categoryId, lastPhotoId, pageSize + 1);

        // 가져온 데이터가 pageSize보다 많으면 다음 페이지가 존재한다.
        boolean hasNext = photos.size() > pageSize;
        // 실제 현재 페이지에 보여줄 사진들만 추출
        List<Photo> currentPagePhotosEntities
            = hasNext ? photos.subList(0, pageSize) : photos;

        List<PhotoDto> photoDtoList = currentPagePhotosEntities.stream()
            .map(this::convertToDto)
            .collect(Collectors.toCollection(ArrayList::new));

        Long nextCursorId = null;
        if (hasNext && !photoDtoList.isEmpty()) {
            // 현재 페이지 마지막 사진 ID
            nextCursorId = photoDtoList.get(photoDtoList.size() -1).getPhotoId();
        }

        return PhotoCursorPageDto.builder()
            .photos(photoDtoList)
            .nextCursorId(nextCursorId)
            .hasNext(hasNext)
            .build();
    }

    // 사진 단일 조회
    // 트랜잭션 안에서 엔티티 변경 시 자동 DB 반영(더티 체킹)
    @Transactional
    public PhotoDto getPhoto(Long photoId) {
        Photo photo = photoRepository.findById(photoId)
            .orElseThrow(() -> new IllegalArgumentException("해당 사진은 존재하지 않습니다."));

        // 조회수 증가
        photo.increaseViews();
        PhotoDto dto = convertToDto(photo);

        // 로그인 사용자 정보 선택적 조회
        Optional<User> currentUser = auth.getOptionalAuth();
        currentUser.ifPresent(user -> {
           Optional<Good> good =
               goodRepository.findByUserIdAndPhoto(currentUser.get().getUserId(), photo);
            if (good.isPresent()) {
                dto.setIsLiked(true);
            }
        });

        return dto;
    }

    // 유저별 좋아요별 사진 목록 조회(Cursor 방식)
    public PhotoCursorPageDto getListPhotoCursorByLike(Long lastPhotoId, int pageSize) {
        // 사용자 인증
        User user = auth.getAuth();

        // 사용자가 좋아요를 한 사진이 있는지 확인
        List<Long> photoGoodList = photoRepository.getListGoodPhotoIdByUserId(user.getUserId());

        // 가져온 데이터가 pageSize보다 많으면 다음 페이지가 존재한다.
        boolean hasNext = photoGoodList.size() > pageSize;

        // 사용자가 좋아요한 사진들 조회
        List<Photo> photos = photoRepository.getListPhotoCursorByLike(photoGoodList);

        // 실제 현재 페이지에 보여줄 사진들만 추출
        List<Photo> currentPagePhotosEntities
            = hasNext ? photos.subList(0, pageSize) : photos;

        List<PhotoDto> photoDtoList = currentPagePhotosEntities.stream()
            .map(this::convertToDto)
            .collect(Collectors.toCollection(ArrayList::new));

        Long nextCursorId = null;
        if (hasNext && !photoDtoList.isEmpty()) {
            // 현재 페이지 마지막 사진 ID
            nextCursorId = photoDtoList.get(photoDtoList.size() -1).getPhotoId();
        }

        return PhotoCursorPageDto.builder()
            .photos(photoDtoList)
            .nextCursorId(nextCursorId)
            .hasNext(hasNext)
            .build();
    }

    // 사진 다운로드
    @Transactional
    public ResponseEntity<InputStreamResource> downloadPhoto(Long photoId) throws Exception {
        // 사용자 인증
        User user = auth.getAuth();

        Photo photo = photoRepository.findById(photoId)
            .orElseThrow(() -> new IllegalArgumentException("해당사진은 존재하지 않습니다."));

        try {
            // 다운로드 횟수 증가
            // @Transactional에 의헤 photo 엔티티의 변경사항은 DB에 자동으로 반영된다.
            // => 영속성 컨텍스트와 더티 체킹 때문
            photo.increaseDownloads();

            // MinIO에서 사진 다운로드 트리거 호출
            InputStream inputStream = minioService.downloadPhoto(photoBucket, photo.getFileName());

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
            .orElseThrow(() -> new IllegalArgumentException("해당 사진은 존재하지 않습니다."));

        try {
            // DB에서 삭제
            photoRepository.delete(photo);

            // Photo 버킷 삭제
            minioService.deleteFile(photoBucket, photo);
            // Thumbnail 버킷 삭제
            minioService.deleteFile(thumbnailBucket, photo);
        } catch (Exception e) {
            log.error("deletePhoto error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private LocalDate getShotDate(MultipartFile multipartFile) {
        LocalDate shotDate = null;

        try (InputStream is = multipartFile.getInputStream()) {
            Metadata metadata = ImageMetadataReader.readMetadata(is);

            ExifSubIFDDirectory directory =
                metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (directory != null) {
                // 촬영일시를 Date 객체로 가지고 온다.
                Date originalDate = directory.getDateOriginal();
                if (originalDate != null) {
                    shotDate =
                        originalDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                }
            }

            return shotDate;
        } catch (IOException | ImageProcessingException e) {
            log.error("getShotDate Method error: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private PhotoDto convertToDto(Photo entity) {
        return PhotoDto.builder()
            .photoId(entity.getPhotoId())
            .fileName(entity.getFileName())
            .location(entity.getLocation())
            .shotDate(entity.getShotDate())
            .bucketFileUrl(cdnEndpoint + "/" + photoBucket + "/" + entity.getFileName())
            .thumbnailUrl(cdnEndpoint + "/" + thumbnailBucket + "/" + getThumbnailName(entity.getFileName()))
            .views(entity.getViews())
            .downloads(entity.getDownloads())
            .likeCount(entity.getGoods().size())
            .categoryId(entity.getCategory() != null ? entity.getCategory().getCategoryId() : null)
            .build();
    }

    private String getThumbnailName(String fileName) {
        String originalName = fileName;
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex != -1) {
            originalName = fileName.substring(0, dotIndex);
        }

        return originalName + ".webp";
    }
}
