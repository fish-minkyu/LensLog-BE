package com.example.LensLog.auth.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// reCAPTCHA v3 전용
@Getter
@Setter
public class RecaptchaResponseDto {
    private boolean success;      // 검증 성공 여부 (Google API 통신 자체의 성공)
    private String challenge_ts; // 타임스탬프
    private String hostname;     // reCAPTCHA가 수행된 호스트
    private String action;       // v3에서 클라이언트가 전달한 action 이름
    private float score;         // v3에서 봇 여부를 나타내는 점수 (0.0 ~ 1.0)
    private List<String> error_codes; // 오류 코드
}
