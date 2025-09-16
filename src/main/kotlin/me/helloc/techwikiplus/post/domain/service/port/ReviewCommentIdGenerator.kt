package me.helloc.techwikiplus.post.domain.service.port

import me.helloc.techwikiplus.post.domain.model.review.ReviewCommentId

interface ReviewCommentIdGenerator {
    fun next(): ReviewCommentId
}
