-- 커서 기반 페이지네이션을 위한 복합 인덱스 추가
-- status와 id를 함께 인덱싱하여 DELETED 상태 필터링과 ID 정렬을 최적화
CREATE INDEX idx_posts_status_id ON posts(status, id DESC);

-- 게시글의 태그 조회 성능 향상을 위한 인덱스
-- 여러 게시글의 태그를 한 번에 조회할 때 사용
CREATE INDEX idx_post_tags_post_id ON post_tags(post_id, display_order);