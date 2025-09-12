package me.helloc.techwikiplus.post.domain.service.port

import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.review.PostReview
import me.helloc.techwikiplus.post.domain.model.review.PostReviewId
import java.time.Instant

interface PostReviewRepository {
    fun save(postReview: PostReview): PostReview

    fun findById(id: PostReviewId): PostReview?

    fun findByPostId(postId: PostId): PostReview?

    fun findAllByPostId(postId: PostId): List<PostReview>

    fun findExpiredReviews(now: Instant): List<PostReview>
}
