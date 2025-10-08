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
    private String authority;
    @Setter
    @Column(nullable = true) // 소셜 로그인일 때만 사용
    private String email;
}
