#
# 빌드 단계 (Build Stage)
#
# OpenJDK 21과 Gradle을 포함하는 이미지를 사용하여 프로젝트를 빌드합니다.
FROM eclipse-temurin:21 AS builder

# 작업 디렉터리를 /app으로 설정합니다.
WORKDIR /app

# Gradle Wrapper 관련 파일들을 복사합니다.
COPY gradlew .
COPY gradle gradle

# build.gradle과 settings.gradle 파일을 복사합니다.
COPY build.gradle .
COPY settings.gradle .

# 의존성 파일을 먼저 캐싱하기 위해 더미 빌드를 실행합니다.
RUN ./gradlew dependencies

# 모든 소스 코드를 복사합니다.
COPY src src

# 프로젝트를 빌드합니다.
RUN ./gradlew bootJar

#
# 실행 단계 (Run Stage)
#
# 경량화된 OpenJDK 21 JRE 런타임 환경 이미지를 사용하여 최종 실행 이미지를 만듭니다.
# 기존 'openjdk:21-jdk-slim' 태그가 존재하지 않아,
# Eclipse Temurin (오픈소스 OpenJDK 배포판)의 JRE 21 slim 이미지로 변경했습니다.
# 'eclipse-temurin:21-jre-jammy'는 Ubuntu 22.04(Jammy Jellyfish) 기반의 경량 JRE 이미지입니다.
FROM eclipse-temurin:21-jre-jammy

# Docker 컨테이너 내부에서 애플리케이션이 사용할 포트를 명시합니다.
EXPOSE 8080

# 'builder' 단계에서 생성된 JAR 파일을 최종 이미지로 복사합니다.
COPY --from=builder /app/build/libs/*.jar app.jar

# 컨테이너 시작 시 실행될 명령어를 정의합니다.
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "/app.jar"]

# Dockerfile 내부에서 설정하고 싶은 환경 변수가 있다면 여기에 추가할 수 있습니다.
# ENV SPRING_PROFILES_ACTIVE=dev