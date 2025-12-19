package com.example.LensLog.auth.controller;

import com.example.LensLog.auth.dto.EmailDto;
import com.example.LensLog.auth.service.EmailService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name="Email Controller", description = "이메일 인증 관련 API")
@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class EmailController {
    private final EmailService emailService;

    // 인증코드 메일 발송
    @PostMapping("/send")
    public ResponseEntity<?> emailSend(@RequestBody EmailDto dto) throws MessagingException {
        emailService.sendVerificationEmail(dto.getEmail());
        return ResponseEntity.ok("이메일 전송 성공");
    }

    // 인증코드 인증
    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody EmailDto dto) {
        if (emailService.verificationCode(dto.getProvider(), dto.getEmail(), dto.getVerifyCode())) {
            return ResponseEntity.ok("인증 성공");
        } else {
            return ResponseEntity.badRequest().body("유효하지 않는 인증코드입니다.");
        }
    }
}
