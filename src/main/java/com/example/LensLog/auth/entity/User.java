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

    private String username;
    @Setter
    private String password;

    private String name;
    @Setter
    @Column(nullable = false)
    private String email;


    @Column(nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    private String provider; // OAuth 전용 컬럼
    private String authority;

    @CreationTimestamp // 엔티티 새성 시 자동으로 현재 시각 저장
    @Column(nullable = false, updatable = false)
    private LocalDate createdDate;
}
