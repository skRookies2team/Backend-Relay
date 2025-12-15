# Backend-Relay Dockerfile (Spring Boot WebFlux with Gradle)
FROM gradle:8.5-jdk17 AS build

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 설정 파일 복사
COPY build.gradle settings.gradle ./
COPY gradle gradle

# 의존성 다운로드 (캐싱 최적화)
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드
RUN gradle bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

# 작업 디렉토리 설정
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 포트 노출 (WebFlux 기본 포트)
EXPOSE 8081

# 환경 변수 설정
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
