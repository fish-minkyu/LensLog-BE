## LensLog - BE

### 스팩

<img src="https://img.shields.io/badge/java-FF160B?style=flat-square&logo=java&logoColor=white"/>
<img src="https://img.shields.io/badge/Spring Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white"/>
<img src="https://img.shields.io/badge/Spring Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white"/>
<img src="https://img.shields.io/badge/JJWT-000000?style=flat-square&logo=&logoColor=white"/>
<img src="https://img.shields.io/badge/Hibernate-59666C?style=flat-square&logo=hibernate&logoColor=white"/>
<img src="https://img.shields.io/badge/Querydsl-008FC7?style=flat-square&logo=querydsl&logoColor=white"/>
<img src="https://img.shields.io/badge/Gmail-EA4335?style=flat-square&logo=gmail&logoColor=white"/>
<img src="https://img.shields.io/badge/MinIO-C72E49?style=flat-square&logo=minio&logoColor=white"/>
<br />
<img src="https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white"/>
<img src="https://img.shields.io/badge/Redis-FF4438?style=flat-square&logo=redis&logoColor=white"/>
<img src="https://img.shields.io/badge/Github Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white"/>
<img src="https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white"/>
<img src="https://img.shields.io/badge/Cloudflare-F38020?style=flat-square&logo=Cloudflare&logoColor=white"/>
<img src="https://img.shields.io/badge/DBeaver-382923?style=flat-square&logo=dbeaver&logoColor=white"/>
<img src="https://img.shields.io/badge/JMeter-D22128?style=flat-square&logo=apachejmeter&logoColor=white"/>
<img src="https://img.shields.io/badge/Figma-F24E1E?style=flat-square&logo=figma&logoColor=white"/>
<img src="https://img.shields.io/badge/Swagger-85EA2D?style=flat-square&logo=swagger&logoColor=white"/>

### 구현 기능

- 로그인 기능
- 소셜 로그인 기능(카카오, 구글, 네이버)
- 카테고리 CRUD
- 사진 기능
    - 사진 CRUD
    - 중복 업로드 방지 기능(해시 알고리즘 + update 락)
    - 사진 다운로드 기능
    - 조회수 기능
    - 썸네일 비동기 생성 + 지수 백오프
    - CDN 기능
    - Reids 캐시 기능
- 좋아요 기능
- 이메일 인증 기능
- Google Recaptcha
- Rate Limiter (전체 API 대상, 초당 5회 요청 제한)
- Swagger