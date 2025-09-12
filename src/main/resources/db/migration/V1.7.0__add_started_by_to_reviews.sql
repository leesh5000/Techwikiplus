-- post_reviews 테이블에 started_by 컬럼 추가
ALTER TABLE post_reviews
    ADD COLUMN started_by BIGINT COMMENT '검수 시작자 ID (비로그인 시 NULL)';