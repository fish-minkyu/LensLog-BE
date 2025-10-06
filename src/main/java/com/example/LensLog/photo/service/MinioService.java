package com.example.LensLog.photo.service;

import com.example.LensLog.photo.entity.Photo;
import io.minio.*;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {
    private final MinioClient minioClient;

    @Value("${minio.bucket.photo.name}")
    private String PHOTO_BUCKET;

    // 사진 업로드 메소드
    @Transactional
    public String savePhotoFile(String storedFileName, MultipartFile multipartFile) throws Exception {
        // 환경변수로 설정한 버킷이 존재한지 확인한다.
        boolean isExist = minioClient.bucketExists(
            BucketExistsArgs.builder()
                .bucket(PHOTO_BUCKET)
                .build()
        );

        // 버킷이 존재하지 않는다면 새로 생성한다.
        if (!isExist) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                .bucket(PHOTO_BUCKET)
                .build());
        }

        // MinIO에 저장한다.
        minioClient.putObject(PutObjectArgs.builder()
            .bucket(PHOTO_BUCKET)
            .object(storedFileName)
            .stream(multipartFile.getInputStream(), multipartFile.getSize(), -1)
            .contentType(multipartFile.getContentType())
            .build());

        // MinIO에 저장된 경로를 반환한다.
        return "/" + storedFileName;
    }

    // 사진 다운로드 메소드
    @Transactional
    public InputStream downloadPhoto(String bucketName, String storedFileName)
        throws
        MinioException,
        IOException,
        NoSuchAlgorithmException,
        InvalidKeyException {
        try {
            InputStream downloadPhoto = minioClient.getObject(
              GetObjectArgs.builder()
                  .bucket(bucketName)
                  .object(storedFileName)
                  .build()
            );

            return downloadPhoto;
        } catch (Exception e) {
            log.error("downloadPhoto error: {}", e.getMessage());
            throw e;
        }
    }

    // 사진 삭제 메소드
    @Transactional
    public void deleteFile(String bucketName, Photo photo) throws Exception {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(photo.getStoredFileName())
                .build()
        );
    }

    // MinIO에서 저장된 파일을 InputStream으로 반환한다.
    public InputStream getFileInputStream(String storedFileName) throws Exception {
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(PHOTO_BUCKET)
                .object(storedFileName)
                .build()
        );
    }

    //
}
