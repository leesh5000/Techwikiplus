-- 검수 요청 테이블
CREATE TABLE post_reviews (
    id BIGINT PRIMARY KEY NOT NULL COMMENT 'ID(PK): Snowflake ID',
    post_id BIGINT NOT NULL COMMENT '게시글 ID (논리적 FK)',
    started_at TIMESTAMP NOT NULL COMMENT '검수 시작 시간',
    deadline TIMESTAMP NOT NULL COMMENT '검수 마감 시간',
    status VARCHAR(20) NOT NULL COMMENT '검수 상태 (IN_PROGRESS, COMPLETED, CANCELLED)',
    winning_revision_id BIGINT COMMENT '선정된 수정본 ID',
    INDEX idx_post_id (post_id)
);

-- 수정본 테이블
CREATE TABLE post_revisions (
    id BIGINT PRIMARY KEY NOT NULL COMMENT 'ID(PK): Snowflake ID',
    review_id BIGINT NOT NULL COMMENT '검수 ID (논리적 FK)',
    author_id BIGINT COMMENT '작성자 ID (비로그인 시 NULL)',
    title VARCHAR(200) NOT NULL COMMENT '수정본 제목',
    body TEXT NOT NULL COMMENT '수정본 내용',
    submitted_at TIMESTAMP NOT NULL COMMENT '제출 시간',
    vote_count INT DEFAULT 0 COMMENT '투표 수',
    INDEX idx_review_id (review_id)
);

-- 투표 테이블
CREATE TABLE revision_votes (
    id BIGINT PRIMARY KEY NOT NULL COMMENT 'ID(PK): Snowflake ID',
    revision_id BIGINT NOT NULL COMMENT '수정본 ID (논리적 FK)',
    voter_id BIGINT COMMENT '투표자 ID (비로그인 시 NULL)',
    voted_at TIMESTAMP NOT NULL COMMENT '투표 시간',
    INDEX idx_revision_id (revision_id)
);