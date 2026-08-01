# Access Token 만료 시 Refresh Token 자동 재발급 구현

## 개요

Access Token이 만료되었을 때 Refresh Token을 사용해 자동으로 재발급하는 로직을 구현했습니다.

---

## 기존 문제점

### 문제 1. JwtTokenFilter가 만료된 토큰을 구분하지 못함
`validate()` 메서드가 만료(`ExpiredJwtException`)와 위변조 등 모든 오류를 동일하게 `false`로 처리했습니다.
만료된 경우에만 Refresh Token을 사용해 재발급해야 하는데, 구분이 불가능했습니다.

### 문제 2. `/api/auth/refresh` 엔드포인트가 인증 필요로 설정됨
`WebSecurityConfig`에서 해당 경로가 `.authenticated()`로 설정되어 있었기 때문에,
Access Token이 만료된 상태에서는 재발급 요청 자체가 **401 Unauthorized**로 차단되었습니다.

---

## 변경 파일

### 1. `JwtTokenUtils.java`

**추가한 메서드:** `isExpiredToken(String token)`

서명은 유효하지만 만료된 토큰인지 판별합니다.
`ExpiredJwtException`이 발생한 경우에만 `true`를 반환하고, 위변조 등 다른 오류는 `false`를 반환합니다.

```java
public boolean isExpiredToken(String token) {
    try {
        jwtParser.parseClaimsJws(token);
        return false;
    } catch (io.jsonwebtoken.ExpiredJwtException e) {
        return true;
    } catch (Exception e) {
        return false;
    }
}
```

---

### 2. `JwtTokenFilter.java`

**변경 내용:**
- 생성자에 `StringRedisTemplate`, `boolean setSecure` 의존성 추가
- `doFilterInternal()` 에 만료 감지 분기 추가
- `tryAutoRefresh()` 메서드 추가 (자동 재발급 핵심 로직)
- `extractRefreshToken()` 메서드 추가
- `addCookie()` 메서드 추가
- 기존 SecurityContext 설정 로직을 `setSecurityContext()` 메서드로 분리

**흐름도:**

```
요청 도착
  │
  ├─ Access Token 유효 → SecurityContext 설정 → 정상 처리
  │
  ├─ Access Token 만료 + 경로가 /api/auth/refresh 가 아닌 경우
  │     │
  │     ├─ Refresh Token 쿠키 추출
  │     ├─ Refresh Token JWT 유효성 검증
  │     ├─ Redis에서 Refresh Token 존재 여부 확인
  │     ├─ 기존 Refresh Token 삭제 (Token Rotation)
  │     ├─ 새 Access Token + Refresh Token 발급
  │     ├─ 쿠키에 새 토큰 설정
  │     └─ SecurityContext 설정 → 정상 처리
  │
  └─ Access Token 없거나 위변조 → SecurityContext 설정 안 함 → 인증 필요 경로면 401
```

**`/api/auth/refresh` 경로를 자동 재발급에서 제외하는 이유:**
필터에서 자동 재발급을 수행하면 Refresh Token이 Redis에서 삭제됩니다.
이후 컨트롤러가 기존 Refresh Token으로 재발급을 시도하면 Redis에 토큰이 없어 오류가 발생합니다.
따라서 해당 경로는 컨트롤러(`AuthController.reIssueTokens()`)가 직접 처리하도록 제외합니다.

---

### 3. `WebSecurityConfig.java`

**변경 내용 1: 의존성 추가**

`JwtTokenFilter`가 `StringRedisTemplate`과 `setSecure`를 필요로 하게 되어 Config에서 주입합니다.

```java
private final StringRedisTemplate redisTemplate;

@Value("${https.secure}")
private boolean setSecure;
```

**변경 내용 2: `JwtTokenFilter` 생성자 호출 수정**

```java
new JwtTokenFilter(
    jwtTokenUtils,
    manager,
    redisTemplate,  // 추가
    setSecure       // 추가
)
```

**변경 내용 3: `/api/auth/refresh` 권한 변경**

```java
// 변경 전
.requestMatchers(HttpMethod.POST, "/api/auth/refresh", "/api/auth/logout", ...)
.authenticated()

// 변경 후
.requestMatchers(HttpMethod.POST, "/api/auth/refresh")
.permitAll()  // Access Token 없이도 직접 갱신 요청 가능
```

---

## `setSecure` 란?

쿠키의 `Secure` 속성을 제어하는 플래그입니다.

| 값 | 동작 |
|---|---|
| `true` | HTTPS 연결에서만 쿠키 전송 (프로덕션) |
| `false` | HTTP에서도 쿠키 전송 (로컬 개발) |

`application.yml` 에서 환경별로 설정합니다:

```yaml
# application-dev.yml
https:
  secure: false

# application-prod.yml
https:
  secure: true
```

---

## Token Rotation 이란?

Refresh Token을 사용할 때마다 기존 토큰을 삭제하고 새 토큰을 발급하는 방식입니다.

```
[사용 전]  Redis: { "uuid-1": "refresh_token_A" }
[재발급 후] Redis: { "uuid-2": "refresh_token_B" }  ← uuid-1 삭제됨
```

**장점:** Refresh Token이 탈취되어 사용되면, 정상 사용자의 재발급 시도에서 Redis 불일치가 감지되어 탈취를 알 수 있습니다.

---

## 최종 토큰 재발급 흐름

```
클라이언트 → API 요청 (만료된 Access Token + 유효한 Refresh Token 쿠키 포함)
     ↓
JwtTokenFilter
  - Access Token 만료 감지
  - Refresh Token Redis 검증
  - 기존 Refresh Token 삭제
  - 새 Access Token + Refresh Token 쿠키 응답에 set
  - SecurityContext 설정
     ↓
Controller → 정상 응답 반환
     ↓
클라이언트 → 새 토큰 쿠키 자동 저장 (브라우저가 Set-Cookie 처리)
```

클라이언트 측에서 별도 처리 없이 **투명하게(transparent)** 토큰이 갱신됩니다.