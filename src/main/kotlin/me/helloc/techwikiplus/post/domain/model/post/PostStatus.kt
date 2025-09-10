package me.helloc.techwikiplus.post.domain.model.post

enum class PostStatus {
    DRAFT, // 미검증
    IN_REVIEW, // 검증 중
    REVIEWED, // 검증 됨
    DELETED, // 삭제
}
