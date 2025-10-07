package com.example.LensLog.photo.entity;

import com.example.LensLog.like.entity.Like;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    // Like와의 OneToMany 관계 설정
    // Photo 하나는 여러 개의 Like를 가질 수 있다.
    @Builder.Default
    @OneToMany(mappedBy = "photo", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore // Jackson이 이 필드를 직렬화하지 않도록 설정
    private List<Like> likes = new ArrayList<>();

    // 조회수 증가 메서드
    public void increaseViews() {
        this.views++;
    }

    // 다운로드 횟수 증가 메서드
    public void increaseDownloads() {
        this.downloads++;
    }

    // Photo에 좋아요 추가
    public void addLike(Like like) {
        this.likes.add(like);
        like.setPhoto(this);
    }

    // Photo에 좋아요 삭제
    public void removeLike(Like like) {
        this.likes.remove(like);
        like.setPhoto(null); // Like 엔티티의 Photo 참조를 끊기
    }
}
