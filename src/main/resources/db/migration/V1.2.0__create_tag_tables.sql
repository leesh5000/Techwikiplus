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

-- 게시글-태그 연결 테이블 (다대다 관계)
CREATE TABLE post_tags (
    post_id BIGINT NOT NULL COMMENT '게시글 ID (논리적 FK -> posts.id)',
    tag_id BIGINT NOT NULL COMMENT '태그 ID (논리적 FK -> tags.id)',
    display_order INTEGER NOT NULL DEFAULT 0 COMMENT '태그 표시 순서 (0-9, 게시글당 최대 10개)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '태그 연결 시점',
    PRIMARY KEY (post_id, tag_id)
) COMMENT '게시글-태그 연결 테이블 (게시글당 최대 10개 태그)';
