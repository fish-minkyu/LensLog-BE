package com.example.LensLog.photo.entity;

import com.example.LensLog.category.entity.Category;
import com.example.LensLog.good.entity.Good;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
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
    // 위치
    private String location;
    // 촬영일
    private LocalDate shotDate;
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

    // Category와의 ManyToOne 관계 설정
    // Photo 여러 개는 하나의 카테고리를 가질 수 있다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonBackReference
    private Category category;

    // Vote와의 OneToMany 관계 설정
    // Photo 하나는 여러 개의 Like를 가질 수 있다.
    @Builder.Default
    @OneToMany(mappedBy = "photo", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore // Jackson이 이 필드를 직렬화하지 않도록 설정
    private List<Good> goods = new ArrayList<>();

    // 조회수 증가 메서드
    public void increaseViews() {
        this.views++;
    }

    // 다운로드 횟수 증가 메서드
    public void increaseDownloads() {
        this.downloads++;
    }

    // Photo에 좋아요 추가
    public void addGood(Good good) {
        this.goods.add(good);
        good.setPhoto(this);
    }

    // Photo에 좋아요 삭제
    public void removeGood(Good good) {
        this.goods.remove(good);
        good.setPhoto(null); // Like 엔티티의 Photo 참조를 끊기
    }
}
