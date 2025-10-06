package com.example.LensLog.photo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long photoId;

    // 원본 파일명
    private String fileName;
    // MinIO에 실제로 저장된 고유 파일명(MinIO 버킷에 중복이름 방지)
    private String storedFileName;
    // 버킷에 저장된 이미지 경로
    private String bucketFileUrl;
    // 해시 값(중복 업로드 방지용)
    private String hashValue;
    // 조회수
    private Long views;
    // 다운로드 횟수
    private Long downloads;


    // 썸네일 이미지 저장된 경로
    private String thumbnailUrl;
    // 썸네일 생성 상태
    private String thumbnailStatus;

    // 조회수 증가 메서드
    public void increaseViews() {
        this.views++;
    }

    // 다운로드 횟수 증가 메서드
    public void increaseDownloads() {
        this.downloads++;
    }
}
