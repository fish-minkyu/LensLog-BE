package com.example.LensLog.auth.dto;

import com.example.LensLog.auth.entity.User;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String email;
    private String password;
    private String name;
    private LocalDate birthDate;
    private String verifyCode;
    private String provider;
    private String authority;

    public static UserDto fromEntity(User entity) {
        return UserDto.builder()
            .email(entity.getEmail())
            .name(entity.getName())
            .birthDate(entity.getBirthDate())
            .provider(entity.getProvider())
            .build();
    }
}
