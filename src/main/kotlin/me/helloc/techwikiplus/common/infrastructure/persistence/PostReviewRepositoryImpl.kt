package me.helloc.techwikiplus.common.infrastructure.persistence

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.PostReviewJpaRepository
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.PostReviewEntity
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.review.PostReview
import me.helloc.techwikiplus.post.domain.model.review.PostReviewId
import me.helloc.techwikiplus.post.domain.model.review.PostReviewStatus
import me.helloc.techwikiplus.post.domain.model.review.PostRevisionId
import me.helloc.techwikiplus.post.domain.service.port.PostReviewRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class PostReviewRepositoryImpl(
    private val jpaRepository: PostReviewJpaRepository,
) : PostReviewRepository {
    override fun save(postReview: PostReview): PostReview {
        val entity = toEntity(postReview)
        val saved = jpaRepository.save(entity)
        return toDomain(saved)
    }

    override fun findById(id: PostReviewId): PostReview? {
        return jpaRepository.findById(id.value).orElse(null)?.let { toDomain(it) }
    }

    override fun findByPostId(postId: PostId): PostReview? {
        return jpaRepository.findByPostId(postId.value)?.let { toDomain(it) }
    }

    override fun findAllByPostId(postId: PostId): List<PostReview> {
        return jpaRepository.findAllByPostIdOrderByStartedAtDesc(postId.value).map { toDomain(it) }
    }

    override fun findExpiredReviews(now: Instant): List<PostReview> {
        return jpaRepository.findExpiredReviews(now).map { toDomain(it) }
    }

    private fun toEntity(domain: PostReview): PostReviewEntity {
        return PostReviewEntity(
            id = domain.id.value,
            postId = domain.postId.value,
            startedAt = domain.startedAt,
            deadline = domain.deadline,
            status = domain.status.name,
            winningRevisionId = domain.winningRevisionId?.value,
            startedBy = domain.startedBy,
        )
    }

    private fun toDomain(entity: PostReviewEntity): PostReview {
        return PostReview(
            id = PostReviewId(entity.id),
            postId = PostId(entity.postId),
            startedAt = entity.startedAt,
            deadline = entity.deadline,
            status = PostReviewStatus.valueOf(entity.status),
            winningRevisionId = entity.winningRevisionId?.let { PostRevisionId(it) },
            startedBy = entity.startedBy,
        )
    }
}
