-- PostReviewStatus enum 값 변경: IN_PROGRESS -> IN_REVIEW
-- 기존 데이터가 있는 경우를 대비하여 UPDATE 수행
UPDATE post_reviews 
SET status = 'IN_REVIEW' 
WHERE status = 'IN_PROGRESS';

-- 컬럼 코멘트 업데이트하여 새로운 enum 값 반영
ALTER TABLE post_reviews 
MODIFY COLUMN status VARCHAR(20) NOT NULL COMMENT '검수 상태 (IN_REVIEW, COMPLETED, CANCELLED)';