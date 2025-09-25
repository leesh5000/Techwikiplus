-- 검수 의견에 변경 제안 필드 추가
ALTER TABLE review_comments
    ADD COLUMN suggested_change TEXT NOT NULL COMMENT '제안된 변경 내용' AFTER comment_type;