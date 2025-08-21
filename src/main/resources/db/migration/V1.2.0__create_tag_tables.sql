-- MySQL RDB Schema for Tags and Post-Tags relationship
-- 태그 제약조건:
-- - 태그명: VARCHAR(30) (한글 10자, 영문 30자 수용)
-- - 게시글당 태그: 최대 10개
-- - 허용 문자: 한글, 영문, 숫자, 하이픈(-), 언더스코어(_)
-- - 정규화: 소문자 변환, 앞뒤 공백 제거
-- - 최소 길이: 2자

-- 태그 마스터 테이블
CREATE TABLE tags (
    id BIGINT PRIMARY KEY NOT NULL COMMENT 'ID(PK): Snowflake ID',
    name VARCHAR(30) NOT NULL COMMENT '태그명 (소문자 정규화, 2-30자, 한글/영문/숫자/-/_만 허용)',
    post_count INT NOT NULL DEFAULT 0 COMMENT '이 태그를 사용하는 게시글 수',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '태그 최초 생성일',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일'
) COMMENT '태그 마스터 테이블';

-- 태그명은 유니크해야 함 (정규화된 이름으로 중복 방지)
CREATE UNIQUE INDEX idx_tag_name ON tags (name);
-- 인기 태그 조회를 위한 인덱스
CREATE INDEX idx_tag_post_count ON tags (post_count DESC);
-- 태그 자동완성을 위한 인덱스
CREATE INDEX idx_tag_name_prefix ON tags (name);

-- 게시글-태그 연결 테이블 (다대다 관계)
CREATE TABLE post_tags (
    post_id BIGINT NOT NULL COMMENT '게시글 ID (논리적 FK -> posts.id)',
    tag_id BIGINT NOT NULL COMMENT '태그 ID (논리적 FK -> tags.id)',
    display_order TINYINT NOT NULL DEFAULT 0 COMMENT '태그 표시 순서 (0-9, 게시글당 최대 10개)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '태그 연결 시점',
    PRIMARY KEY (post_id, tag_id)
) COMMENT '게시글-태그 연결 테이블 (게시글당 최대 10개 태그)';

-- 태그별 게시글 조회를 위한 인덱스
-- 사용 사례: 
-- 1) 특정 태그가 달린 모든 게시글 조회: WHERE tag_id = ? ORDER BY created_at DESC
-- 2) 태그 클릭 시 해당 태그의 최신 게시글 목록 표시
-- 3) 태그별 게시글 수 집계: SELECT COUNT(*) FROM post_tags WHERE tag_id = ?
CREATE INDEX idx_post_tags_tag_id ON post_tags (tag_id, created_at DESC);

-- 게시글별 태그를 순서대로 조회하기 위한 인덱스
-- 사용 사례:
-- 1) 게시글 상세 조회 시 태그 목록 표시: WHERE post_id = ? ORDER BY display_order
-- 2) 게시글 목록에서 각 게시글의 태그 미리보기 (상위 3개만 표시 등)
-- 3) 게시글 수정 시 기존 태그 목록 조회
CREATE INDEX idx_post_tags_display_order ON post_tags (post_id, display_order ASC);