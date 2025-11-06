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
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

// 메일 관련 비즈니스 로직
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    // 인증 메일 보내기
    public void sendVerificationEmail(String toEmail) throws MessagingException {
        String title = "[Lens Log] 이메일 인증 코드 안내";
        String content = "<h1>이메일 인증 코드 안내입니다.</h1>" +
                        "<p>안녕하세요. [서비스명] 입니다.</p>" +
                        "<p>아래 코드를 입력하여 이메일 인증을 완료해주세요.</p>" +
                        "<h2 style='color:#007bff;'>" + generationRandomCode(toEmail) + "</h2>" +
                        "<p>본 코드는 5분간 유효합니다.</p>" +
                        "<p>감사합니다.</p>";

        sendEmail(toEmail, title, content);
    }

    // 메일 보내기
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

        // 이미 인증코드를 생성되었다면 삭제
        if (Boolean.TRUE.equals(redisTemplate.hasKey(toEmail))) {
            redisTemplate.delete(toEmail);
        }

        // Redis에 5분간 유효한 인증코드 저장
        ValueOperations<String, String> operations = redisTemplate.opsForValue();
        operations.set(toEmail, randomCode.toString(), 5, TimeUnit.MINUTES);

        return randomCode.toString();
    }

    // 인증 코드 유효성 검사
    public boolean verificationCode(String toEmail, String code) {
        if (Boolean.FALSE.equals(redisTemplate.hasKey(toEmail))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        String emailCode = redisTemplate.opsForValue().get(toEmail);
        return code != null && code.equals(emailCode);
    }
}
