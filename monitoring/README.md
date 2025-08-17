# TechWikiPlus 모니터링 시스템

이 프로젝트는 Prometheus와 Grafana를 이용한 종합적인 모니터링 시스템을 제공합니다.

## 구성 요소

### Prometheus
- **포트**: 9090 (기본값, `.env`에서 변경 가능)
- **역할**: 메트릭 수집 및 저장
- **수집 대상**:
  - Spring Boot 애플리케이션 (Actuator 엔드포인트)
  - Prometheus 자체 메트릭
  - 인프라 컴포넌트 (Redis, MySQL - exporter 필요)

### Grafana
- **포트**: 3000 (기본값, `.env`에서 변경 가능)
- **기본 계정**: admin / admin123! (`.env`에서 변경 가능)
- **사전 구성된 대시보드**:
  1. **Spring Boot Metrics**: JVM, HTTP 요청, 데이터베이스 연결
  2. **Infrastructure Status**: 서비스 상태, Prometheus 메트릭
  3. **Application Metrics**: API 사용량, 에러율, 비즈니스 메트릭

## 시작하기

### 1. 환경 변수 설정
```bash
# .env 파일 생성
cp .env.example .env

# 필요시 포트 및 패스워드 변경
vim .env
```

### 2. 모니터링 서비스 시작
```bash
# 인프라 서비스만 시작 (모니터링 포함)
docker-compose -f docker-compose.infra.yml up -d

# 또는 전체 서비스 시작
docker-compose up -d
```

### 3. 접속 확인
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000
  - 계정: admin / admin123!

## 대시보드 설명

### Spring Boot Metrics
- **JVM Heap Memory**: 힙 메모리 사용량 모니터링
- **HTTP Request Rate**: 요청 처리율 및 상태 코드별 분류
- **HTTP Response Time**: 응답 시간 분포 (95th, 50th percentile)
- **Database Connections**: HikariCP 커넥션 풀 상태

### Infrastructure Status
- **Service Status**: 각 서비스의 Up/Down 상태
- **Prometheus Metrics**: 메트릭 수집 현황

### Application Metrics
- **API Usage**: 엔드포인트별 사용량
- **Error Rate**: 에러율 게이지
- **Request Distribution**: 상태 코드별 요청 분포

## 알림 규칙

다음 상황에서 알림이 발생합니다:

### Critical Alerts
- 애플리케이션 다운 (1분 이상)
- 높은 에러율 (5% 초과, 3분 이상)
- 데이터베이스 연결 실패
- Redis/MySQL 서비스 다운

### Warning Alerts  
- 높은 메모리 사용률 (80% 초과, 5분 이상)
- 높은 응답 시간 (2초 초과, 3분 이상)

## Spring Boot 설정

애플리케이션에서 다음 설정이 필요합니다:

```yaml
management:
  server:
    port: 9090  # Prometheus가 이 포트에서 메트릭 수집
  endpoints:
    web:
      exposure:
        include:
          - health
          - metrics  
          - prometheus  # 필수!
```

## 트러블슈팅

### 메트릭이 수집되지 않는 경우
1. Spring Boot 애플리케이션의 `/actuator/prometheus` 엔드포인트 확인
2. Prometheus 타겟 상태 확인: http://localhost:9090/targets
3. Docker 네트워크 연결 확인

### Grafana 대시보드가 비어있는 경우
1. Prometheus 데이터소스 연결 확인
2. 메트릭 데이터 수집 확인
3. 대시보드 쿼리 유효성 검증

### 성능 최적화
- Prometheus 스토리지 설정 조정
- 메트릭 수집 간격 조정 (`scrape_interval`)
- Grafana 쿼리 최적화

## 확장

### 추가 Exporter
- **node_exporter**: 시스템 메트릭
- **redis_exporter**: Redis 상세 메트릭  
- **mysqld_exporter**: MySQL 상세 메트릭

### 커스텀 메트릭
Spring Boot 애플리케이션에서 Micrometer를 이용해 비즈니스 메트릭 추가 가능:

```kotlin
@Component
class CustomMetrics(private val meterRegistry: MeterRegistry) {
    private val userLoginCounter = Counter.builder("user.login.total")
        .description("Total user logins")
        .register(meterRegistry)
        
    fun recordUserLogin() {
        userLoginCounter.increment()
    }
}
```

## 보안 고려사항

1. **운영 환경에서는 반드시 비밀번호 변경**
2. **네트워크 접근 제한** (방화벽, VPN)
3. **HTTPS 설정** (reverse proxy 사용)
4. **인증/인가 강화** (LDAP, OAuth 등)