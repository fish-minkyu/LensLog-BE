package com.example.LensLog.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;


@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    // LensLog 유저명
    private String username;

    // 비밀번호
    @Setter
    private String password;

    // 사용자 이름
    private String name;

    // 사용자 이메일
    @Setter
    @Column(nullable = false)
    private String email;

    // 인증 여부
    @Column(nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    // 로컬 로그인 or 소셜 로그인(네이버, 카카오, 구글)
    private String provider; // OAuth 전용 컬럼
    private String authority;

    @CreationTimestamp // 엔티티 새성 시 자동으로 현재 시각 저장
    @Column(nullable = false, updatable = false)
    private LocalDate createdDate;
}
