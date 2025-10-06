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

    public static UserDto fromEntity(User entity) {
        return UserDto.builder()
            .username(entity.getUsername())
            .build();
    }
}
