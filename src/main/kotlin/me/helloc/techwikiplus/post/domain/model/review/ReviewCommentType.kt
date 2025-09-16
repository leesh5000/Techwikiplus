package me.helloc.techwikiplus.post.domain.model.review

enum class ReviewCommentType(
    val description: String,
) {
    INACCURACY("부정확한 내용"),
    NEEDS_IMPROVEMENT("개선이 필요한 내용"),
}
