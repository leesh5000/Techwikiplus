-- MySQL RDB Schema for Post
CREATE TABLE posts (
                       id BIGINT PRIMARY KEY NOT NULL COMMENT 'ID(PK): Snowflake ID',
                       title VARCHAR(200) NOT NULL COMMENT '문서 제목 (최대 200자)',
                       body TEXT NOT NULL COMMENT '문서 내용 (최대 50000자)',
                       status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '문서 상태 (DRAFT, IN_REVIEW, REVIEWED)',
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일'
);
