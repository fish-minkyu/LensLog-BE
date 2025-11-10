package com.example.LensLog.photo.controller;

import com.example.LensLog.photo.dto.PhotoCursorPageDto;
import com.example.LensLog.photo.dto.PhotoDto;
import com.example.LensLog.photo.service.PhotoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@Tag(name = "Photo Controller", description = "사진 관련 API")
@RestController
@RequestMapping("/api/photo")
@RequiredArgsConstructor
public class PhotoController {
    private final PhotoService photoService;

    // 사진 생성
    @PostMapping("/upload")
    public void uploadPhoto(
        @RequestParam("file") MultipartFile file
        ) throws Exception {
        photoService.uploadPhoto(file);
    }

    // 사진 목록 조회(Cursor)
    @GetMapping("/getList")
    public PhotoCursorPageDto getListPhotoCursor(
        @RequestParam(name = "lastPhotoId", required = false) Long lastPhotoId,
        // PageableDefault는 주로 offset 기반 페이징에서 사용된다.
        @RequestParam(name = "pageSize", defaultValue = "10") int pageSize
        ) {
        return photoService.getListPhotoCursor(lastPhotoId, pageSize);
    }

    // 사진 단일 조회
    @GetMapping("/getOne/{photoId}")
    public PhotoDto getPhoto(@PathVariable("photoId") Long photoId) {
        return photoService.getPhoto(photoId);
    }

    // 사진 다운로드
    @GetMapping("/download/{photoId}")
    public ResponseEntity<InputStreamResource> downloadPhoto(
        @PathVariable("photoId") Long photoId
    ) throws Exception {
        return photoService.downloadPhoto(photoId);
    }

    // 사진 삭제
    @DeleteMapping("/delete/{photoId}")
    public void deletePhoto(
        @PathVariable("photoId") Long photoId
    ) throws Exception {
        photoService.deletePhoto(photoId);
    }
}
