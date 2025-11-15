package com.example.LensLog.auth.entity;

import jakarta.persistence.*;
import lombok.*;


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
    private String password;

    @Column(nullable = false)
    private String name;
    @Setter
    @Column(nullable = false)
    private String email;


    @Column(nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    private String provider; // OAuth 전용 컬럼
    private String authority;
}
