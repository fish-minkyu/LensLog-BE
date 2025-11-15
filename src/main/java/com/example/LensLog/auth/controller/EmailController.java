package com.example.LensLog.auth.controller;

import com.example.LensLog.auth.dto.EmailDto;
import com.example.LensLog.auth.service.EmailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class EmailController {
    private final EmailService emailService;

    // 인증코드 메일 발송
    @PostMapping("/send")
    public ResponseEntity<?> emailSend(@RequestBody EmailDto dto) throws MessagingException {
        emailService.sendVerificationEmail(dto.getEmail());
        return ResponseEntity.ok("Sending mail is success");
    }

    // 인증코드 인증
    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody EmailDto dto) {
        if (emailService.verificationCode(null, dto.getEmail(), dto.getVerifyCode())) {
            return ResponseEntity.ok("The verification is success");
        } else {
            return ResponseEntity.badRequest().body("Invalid verification code.");
        }
    }
}
