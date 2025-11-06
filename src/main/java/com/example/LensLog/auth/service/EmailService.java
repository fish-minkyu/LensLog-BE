package com.example.LensLog.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

// 메일 관련 비즈니스 로직
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private static final String EMAIL_PREFIX = "email_verification";

    // 인증 메일 보내기
    public void sendVerificationEmail(String toEmail) throws MessagingException {
        String title = "[Lens Log] 이메일 인증 코드 안내";
        String content = "<h1>이메일 인증 코드 안내입니다.</h1>" +
                        "<p>안녕하세요. Lens Log 입니다.</p>" +
                        "<p>아래 코드를 입력하여 이메일 인증을 완료해주세요.</p>" +
                        "<h2 style='color:#007bff;'>" + generationRandomCode(toEmail) + "</h2>" +
                        "<p>본 코드는 5분간 유효합니다.</p>" +
                        "<p>감사합니다.</p>";

        sendEmail(toEmail, title, content);
    }

    // 메일 보내기
    @Async
    public void sendEmail(String toEmail, String title, String content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(toEmail);
        helper.setSubject(title);
        helper.setText(content, true); // true를 설정해서 HTML을 사용 가능하게 한다.
        helper.setReplyTo("eo.minkyu1219@gmail.com");

        try {
            mailSender.send(message);
        } catch (RuntimeException e) {
            log.error("Send a email has an error: {}", e.getMessage());
            throw new RuntimeException("Unable to send email in sendEmail", e);
        }
    }

    // 인증 코드 생성 및 Redis에 저장
    public String generationRandomCode(String toEmail) {
        // 숫자 + 대문자 + 소문자
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        
        StringBuilder randomCode = new StringBuilder();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(characters.length());
            randomCode.append(characters.charAt(index));
        }

        // Redis에 5분간 유효한 인증코드 저장
        ValueOperations<String, String> operations = redisTemplate.opsForValue();
        String redisKey = EMAIL_PREFIX + toEmail;
        operations.set(redisKey, randomCode.toString(), 5, TimeUnit.MINUTES);

        return randomCode.toString();
    }

    // 인증 코드 유효성 검사
    public boolean verificationCode(String toEmail, String verifyCode) {
        String redisKey = EMAIL_PREFIX + toEmail;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(redisKey))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        String storedCode = redisTemplate.opsForValue().get(redisKey);
        if (storedCode != null && storedCode.equals(verifyCode)) {
            redisTemplate.delete(redisKey);
            return true;
        }

        return false;
    }

    public void deleteVerificationCode(String toEmail) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(EMAIL_PREFIX + toEmail))) {
            redisTemplate.delete(toEmail);
            log.info("인증 코드 삭제 완료: {}", toEmail);
        } else {
            log.warn("삭제할 인증 코드가 Redis에 없습니다: {}", toEmail);
        }
    }
}
