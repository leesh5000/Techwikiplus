CREATE TABLE post_histories (
    id BIGINT PRIMARY KEY NOT NULL,
    post_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    change_type VARCHAR(20) NOT NULL,
    changed_at TIMESTAMP NOT NULL,
    review_id BIGINT,
    revision_id BIGINT,
    changed_by BIGINT,
    INDEX idx_post_id (post_id),
    INDEX idx_changed_at (changed_at)
);