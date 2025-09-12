package me.helloc.techwikiplus.common.infrastructure.persistence.jpa

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.PostReviewEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface PostReviewJpaRepository : JpaRepository<PostReviewEntity, Long> {
    fun findByPostId(postId: Long): PostReviewEntity?

    fun findAllByPostIdOrderByStartedAtDesc(postId: Long): List<PostReviewEntity>

    @Query("SELECT r FROM PostReviewEntity r WHERE r.deadline < :now AND r.status = 'IN_REVIEW'")
    fun findExpiredReviews(now: Instant): List<PostReviewEntity>
}
