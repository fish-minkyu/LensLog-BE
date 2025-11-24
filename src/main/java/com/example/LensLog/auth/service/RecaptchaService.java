package com.example.LensLog.auth.service;

import com.example.LensLog.auth.dto.RecaptchaResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecaptchaService {
    private final RestTemplate restTemplate;

    @Value("${recaptcha.secret-key}")
    private String recaptchaSecretKey;

    @Value("${recaptcha.verify-url}")
    private String recaptchaVerifyUrl;

    // reCAPTCHA 토큰을 Google API에 전송하여 검증하고, RecaptchaResponse 객체를 반환
    // v3의 경우 점수(score)와 액션(action) 정보가 포함된다.
    public RecaptchaResponseDto verifyRecaptcha(String recaptchaResponse) {
        MultiValueMap<String, String> requestMap = new LinkedMultiValueMap<>();
        requestMap.add("secret", recaptchaSecretKey);
        requestMap.add("response", recaptchaResponse); // 프론트에서 받은 토큰

        try {
            return restTemplate
                .postForObject(recaptchaVerifyUrl, requestMap, RecaptchaResponseDto.class);
        } catch (Exception e) {
            log.error("Recaptcha 인증 시 오류 발생: {}", e.getMessage());
            // 오류 발생 시 실패 응답 객체를 반환
            RecaptchaResponseDto errorResponse = new RecaptchaResponseDto();
            errorResponse.setSuccess(false);
            // 필요에 따라 error_codes 필드도 설정 가능
            return errorResponse;
        }
    }
}
