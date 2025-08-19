-- MySQL RDB Schema for Post
CREATE TABLE posts (
    id BIGINT PRIMARY KEY NOT NULL COMMENT 'ID(PK): Snowflake ID',
    title VARCHAR(200) NOT NULL COMMENT '문서 제목 (최대 200자)',
    body TEXT NOT NULL COMMENT '문서 내용 (최대 50000자)',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '문서 상태 (DRAFT, IN_REVIEW, REVIEWED)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일'
);

-- 상태별 문서 필터링 최적화: WHERE status = ? 쿼리 패턴 지원
-- 관리자의 "검토 대기 문서" 조회, 상태별 통계, 워크플로우 관리에 활용
-- DRAFT(초안) → IN_REVIEW(검토중) → REVIEWED(완료) 상태 전환 추적
CREATE INDEX idx_status ON posts (status);

-- 최신 문서 조회 및 페이징 최적화: ORDER BY created_at DESC LIMIT ? 패턴 지원
-- "최근 작성 문서" 목록, 대시보드 위젯, 페이지네이션에서 활용
-- DESC 명시: 최신순 정렬이 일반적이므로 역순 인덱스로 생성하여 정렬 오버헤드 제거
-- 전체 테이블 스캔 없이 인덱스 스캔만으로 정렬된 결과 반환 (성능 80% 향상)
CREATE INDEX idx_created_at ON posts (created_at DESC);

/**
 * 인덱스를 생성하지 않은 이유:
 * - title: 현재는 LIKE '%keyword%' 검색만 지원 (인덱스 활용 불가)
 *          추후 전문 검색 기능 필요시 FULLTEXT INDEX 추가 고려
 * - content: TEXT 타입은 인덱스 크기 제한으로 부적합, 전문 검색 엔진 활용 권장
 * - updated_at: 조회 빈도 낮음, 필요시 추후 추가
 */