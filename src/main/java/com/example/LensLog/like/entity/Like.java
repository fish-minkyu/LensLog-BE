package com.example.LensLog.like.entity;

import com.example.LensLog.photo.entity.Photo;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Like {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long likeId;

    private Long userId;

    // Photo와의 ManyToOne 관계 설정
    // Like 테이블의 "photo_id" 컬럼이 Photo 엔티티의 기본 키를 참조하도록 연결한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_id", nullable = false)
    private Photo photo;
}
