-- Add version column to posts table for business version tracking
ALTER TABLE posts
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '문서 버전' AFTER status;