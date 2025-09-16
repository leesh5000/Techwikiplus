-- 검수 의견 테이블
CREATE TABLE review_comments (
    id BIGINT PRIMARY KEY NOT NULL COMMENT 'ID(PK): Snowflake ID',
    revision_id BIGINT NOT NULL COMMENT '수정본 ID (논리적 FK)',
    line_number INT NOT NULL COMMENT '의견이 달린 라인 번호',
    comment TEXT NOT NULL COMMENT '검수 의견 내용',
    comment_type VARCHAR(50) NOT NULL COMMENT '의견 유형 (INACCURACY, NEEDS_IMPROVEMENT)',
    created_at TIMESTAMP NOT NULL COMMENT '생성 시간',
    INDEX idx_revision_id (revision_id)
);