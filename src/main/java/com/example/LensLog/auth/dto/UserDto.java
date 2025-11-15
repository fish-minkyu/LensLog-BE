package com.example.LensLog.auth.dto;

import com.example.LensLog.auth.entity.User;
import lombok.*;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String username;
    private String password;
    private String name;
    private String email;
    private String verifyCode;
    private String provider;
    private String authority;

    public static UserDto fromEntity(User entity) {
        return UserDto.builder()
            .username(entity.getUsername())
            .name(entity.getName())
            .email(entity.getEmail())
            .provider(entity.getProvider())
            .build();
    }
}
