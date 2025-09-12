package me.helloc.techwikiplus.post.domain.service.port

import me.helloc.techwikiplus.post.domain.model.review.PostReviewId
import me.helloc.techwikiplus.post.domain.model.review.PostRevision
import me.helloc.techwikiplus.post.domain.model.review.PostRevisionId

interface PostRevisionRepository {
    fun save(postRevision: PostRevision): PostRevision

    fun findById(id: PostRevisionId): PostRevision?

    fun findByReviewId(reviewId: PostReviewId): List<PostRevision>
}
