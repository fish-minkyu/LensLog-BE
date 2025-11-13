package com.example.LensLog.auth.jwt;

import com.example.LensLog.auth.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.security.Key;
import java.util.Date;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// JWT 자체와 관련된 기능을 만드는 곳
@Slf4j
@Component
public class JwtTokenUtils {
    // JWT를 만드는 용도의 암호키
    private final Key signingKey;
    // JWT를 해석하는 용도의 객체
    private final JwtParser jwtParser; // parser: 특정한 형식의 문자열을 데이터로 다시 역직렬화하는 것
    // Refresh Token을 Redis에 관리하기 위한 객체
    private final StringRedisTemplate redisTemplate;


    public JwtTokenUtils(
        @Value("${jwt.secret}")
        String jwtSecret,
        StringRedisTemplate redisTemplate
    ) {
        log.info(jwtSecret);
        // jjwt에서 key를 활용하기 위한 준비
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        this.jwtParser = Jwts
            .parserBuilder()
            .setSigningKey(this.signingKey)
            .build();
        this.redisTemplate = redisTemplate;
    }

    // UserDetails를 받아서 JWT로 변환하는 메서드
    // UserDetails를 받아서 사용하는 이유는, Spring Security에서 UserDetails를 사용하고 있기 때문이다.
    public String generateToken(CustomUserDetails customUserDetails) {
        // JWT에 담고싶은 정보를 Claims로 만든다.
        // sub: 누구인지
        // iat: 언제 발급 되었는지
        // exp: 언제 만료 예정인지
        // => 이 3가지 정보는 거의 표준으로 추가한다.

        // 현재 호출되었을 때 epoch time을 받아오는 메서드
        Instant now = Instant.now();
        Claims jwtClaims = Jwts.claims() // 일종의 Builder처럼 동작한다.
            // sub: 누구인지
            .setSubject(customUserDetails.getEmail())

            // setIssuedAt
            // : 자바의 Date 클래스를 받는다.
            // 날짜를 나타내기 위한 용도

            // iat: 언제 발급 되었는지
            .setIssuedAt(Date.from(now))
            // exp: 언제 만료 예정인지
            .setExpiration(Date.from(now.plusSeconds(60 * 60 * 24))); // 하루

        //TODO authorities 엔티티 or Enum을 만들어서 추가해줘야 한다.
  /*
      jwtClaims.put("test", "claims");
  */

        // 최종적으로 JWT를 발급한다.
        return Jwts.builder()
            .setClaims(jwtClaims)
            .signWith(this.signingKey)
            .compact();
    }

    // Refresh Token을 발급하는 메서드
    public String generateRefreshToken(CustomUserDetails customUserDetails) {
        ValueOperations<String, String> operations = redisTemplate.opsForValue();

        // 현재 호출되었을 때 epoch time
        Instant now = Instant.now();
        // 다중세션 관리, 동일 사용자가 클라이언트별 동시 접속이 가능하게 설정
        String refreshTokenId = UUID.randomUUID().toString();

        Claims jwtClaims = Jwts.claims()
            .setId(refreshTokenId)
            .setSubject(customUserDetails.getEmail())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusSeconds(60 * 60 * 24 * 7))); // 일주일

        // Refresh Token을 발급한다.
        String refreshToken = Jwts.builder()
            .setClaims(jwtClaims)
            .signWith(this.signingKey)
            .compact();

        // Refresh 토큰을 Redis에 저장
        operations.set(refreshTokenId, refreshToken, 7, TimeUnit.DAYS); // 7일간 저장

        return refreshToken;
    }

    // 정상적인 JWT인지를 판단하는 메서드
    public boolean validate(String token) {
        try {
            // Json Web Signature
            // : Signature까지 확인하며, 정상적이지 않은 JWT라면 예외(Exception)가 발생한다.
            jwtParser.parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.warn("invalid jwt");
        }
        return false;
    }

    // 실제 데이터(Payload)를 반환하는 메서드
    public Claims parseClaims(String token) {
        return jwtParser
            .parseClaimsJws(token)
            .getBody();
    }

    // 토큰에서 사용자 메일을 추출하는 메소드
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    // Refresh Token에서 Redis의 key를 추출하는 메소드
    public String extractRediskey(String token) {
        return parseClaims(token).getId();
    }

    // 토큰 만료 여부를 확인하는 메소드
    public boolean isTokenExpired(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration.before(new Date(System.currentTimeMillis()));
    }

    // 특정 사용자 정보와 토큰의 유효성 (만료 여부, 사용자 일치)을 함께 검증하는 메소드
    public boolean validateToken(String token, CustomUserDetails customUserDetails) {
        final String email = extractEmail(token);
        return (email.equals(customUserDetails.getEmail()) && !isTokenExpired(token));
    }

    // Redis에서 Refresh Token을 삭제하는 메소드
    public void deleteRefreshToken(String redisKey) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            redisTemplate.delete(redisKey);
        }
    }
}