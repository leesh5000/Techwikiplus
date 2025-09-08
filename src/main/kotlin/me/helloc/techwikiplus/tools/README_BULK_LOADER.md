# Bulk Post Data Loader 사용 가이드

## 개요
이 도구는 Techwikiplus 애플리케이션에 2000만 건의 게시글 데이터를 효율적으로 삽입하기 위한 유틸리티입니다.

## 주요 기능
- 2000만 건의 게시글 데이터 자동 생성 및 삽입
- 100만 건마다 진행 상황 출력
- 병렬 처리 (8개 워커)로 성능 최적화
- JDBC 배치 처리로 빠른 삽입

## 실행 방법

### 방법 1: IntelliJ IDEA에서 실행
1. `BulkDataLoaderRunner.kt` 파일 열기
2. main 함수 왼쪽의 실행 버튼 클릭
3. 또는 클래스에서 우클릭 → Run 'BulkDataLoaderRunner'

### 방법 2: 터미널에서 Gradle로 실행
```bash
# 2000만 건 삽입 (기본값)
./gradlew bootRun --args="--spring.profiles.active=bulk-loader"

# 특정 개수 삽입 (예: 100만 건)
./gradlew bootRun --args="--spring.profiles.active=bulk-loader 1000000"
```

### 방법 3: 테스트 코드에서 실행
```kotlin
@SpringBootTest
@ActiveProfiles("bulk-loader")
class BulkLoaderTest {
    @Autowired
    lateinit var bulkPostDataLoader: BulkPostDataLoader
    
    @Test
    @Disabled("수동 실행용")
    fun loadData() {
        // 1000건 테스트
        bulkPostDataLoader.loadPosts(1000)
        
        // 또는 2000만 건
        // bulkPostDataLoader.loadPosts(20_000_000)
    }
}
```

### 방법 4: 애플리케이션 코드에서 직접 호출
```kotlin
@Component
class MyDataInitializer(
    private val bulkPostDataLoader: BulkPostDataLoader
) {
    @PostConstruct
    fun init() {
        // 필요시 조건부로 실행
        if (shouldLoadData()) {
            bulkPostDataLoader.loadPosts(20_000_000)
        }
    }
}
```

## 실행 전 확인사항

### 1. 데이터베이스 준비
```bash
# Docker Compose로 DB 시작
docker-compose -f docker-compose.infra.yml up -d mysql
```

### 2. 테이블 생성 확인
Flyway 마이그레이션이 실행되어 posts 테이블이 생성되어 있어야 합니다.

### 3. 디스크 공간
- 최소 10-20GB의 여유 공간 필요
- MySQL 데이터 디렉토리 위치 확인

### 4. 메모리 설정 (권장)
```bash
# JVM 메모리 옵션 추가
export JAVA_OPTS="-Xms4g -Xmx8g -XX:+UseG1GC"
./gradlew bootRun --args="--spring.profiles.active=bulk-loader"
```

## 진행 상황 모니터링

실행 중 100만 건마다 다음과 같은 진행 상황이 출력됩니다:

```
===== 대량 데이터 삽입 진행 현황 =====
처리 완료: 1,000,000 / 20,000,000 건 (5.0%)
소요 시간: 2분 30초
처리 속도: 6,667 건/초
예상 남은 시간: 47분 30초
=====================================
```

## 성능 정보

### 예상 처리 속도
- 일반 PC: 3,000-5,000 건/초
- 서버급 하드웨어: 8,000-15,000 건/초

### 예상 소요 시간
- 100만 건: 3-5분
- 1000만 건: 30-40분
- 2000만 건: 40-60분

### 리소스 사용량
- CPU: 8코어 기준 60-80% 사용
- 메모리: 4-8GB
- 디스크 I/O: 높음

## 데이터 특성

### 생성되는 데이터
- **제목**: 50가지 기술 주제 템플릿 중 랜덤 선택
- **본문**: 실제 기술 문서 스타일 (500-2000자)
- **상태**: REVIEWED (70%), IN_REVIEW (20%), DRAFT (10%)
- **생성 시간**: 최근 1년간 랜덤 분포
- **ID**: Snowflake 알고리즘으로 생성

## 주의사항

1. **프로덕션 환경에서 실행 금지**
   - 개발/테스트 환경에서만 사용

2. **기존 데이터 백업**
   - 실행 전 필요시 데이터 백업

3. **인덱스 영향**
   - 대량 삽입 후 인덱스 재구성 필요할 수 있음

4. **트랜잭션 로그**
   - MySQL binlog 크기 증가 주의

## 문제 해결

### OutOfMemoryError 발생
```bash
# 메모리 증가
export JAVA_OPTS="-Xms8g -Xmx16g"
```

### 연결 풀 부족
`application-bulk-loader.yml`에서 hikari pool 크기 조정

### 속도가 너무 느림
- 배치 크기 조정 (BATCH_SIZE)
- 병렬 워커 수 조정 (PARALLEL_WORKERS)
- 데이터베이스 서버 스펙 확인

## 데이터 삭제

모든 게시글 데이터를 삭제하려면:

```kotlin
bulkPostDataLoader.clearAllPosts()
```

또는 SQL 직접 실행:
```sql
DELETE FROM posts;
```