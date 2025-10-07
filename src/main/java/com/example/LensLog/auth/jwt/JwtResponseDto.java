package com.example.LensLog.auth.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 단순 Dto여서 Data를 넣어줬다.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponseDto {
  private String accessToken;
  private String refreshToken;
}