# =============================================================================
# Multi-stage Dockerfile for Spring Boot Kotlin Application
# =============================================================================

# -----------------------------------------------------------------------------
# Build Stage: Gradle 빌드 환경
# -----------------------------------------------------------------------------
FROM gradle:8.10.2-jdk21-alpine AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle wrapper와 설정 파일들 먼저 복사 (캐시 최적화)
COPY gradle/ gradle/
COPY gradlew settings.gradle.kts build.gradle.kts ./

# 의존성 미리 다운로드 (소스 변경 시 재다운로드 방지)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src/ src/

# 애플리케이션 빌드 (테스트 제외로 빌드 시간 단축)
RUN ./gradlew bootJar -x test --no-daemon

# JAR 파일 레이어 추출 (Spring Boot 2.3+ 레이어 기능)
RUN java -Djarmode=layertools -jar build/libs/*.jar extract

# -----------------------------------------------------------------------------
# Runtime Stage: 최적화된 프로덕션 실행 환경
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# 시스템 패키지 업데이트 및 필수 패키지 설치
RUN apk update && \
    apk add --no-cache \
        curl \
        tzdata && \
    rm -rf /var/cache/apk/*

# 타임존 설정
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 애플리케이션 전용 사용자 생성 (보안 강화)
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# 애플리케이션 디렉토리 생성
WORKDIR /app

# 빌드 단계에서 추출한 레이어들을 순서대로 복사 (Docker 레이어 캐싱 최적화)
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

# 소유권 변경
RUN chown -R appuser:appgroup /app

# 비루트 사용자로 전환
USER appuser

# 포트 노출
EXPOSE 9000 9001

# JVM 최적화 설정
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -XX:+OptimizeStringConcat \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.backgroundpreinitializer.ignore=true"

# 헬스체크 설정
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:9001/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]

# 메타데이터 레이블
LABEL maintainer="TechWikiPlus Team" \
      version="0.0.1-SNAPSHOT" \
      description="TechWikiPlus Spring Boot Application" \
      org.opencontainers.image.source="https://github.com/your-org/techwikiplus" \
      org.opencontainers.image.documentation="https://github.com/your-org/techwikiplus/README.md"