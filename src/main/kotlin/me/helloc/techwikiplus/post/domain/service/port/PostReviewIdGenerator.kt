package me.helloc.techwikiplus.post.domain.service.port

import me.helloc.techwikiplus.post.domain.model.review.PostReviewId

interface PostReviewIdGenerator {
    fun generate(): PostReviewId
}
