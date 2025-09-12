package me.helloc.techwikiplus.common.infrastructure.persistence

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.PostHistoryJpaRepository
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.PostHistoryEntity
import me.helloc.techwikiplus.post.domain.model.history.PostChangeType
import me.helloc.techwikiplus.post.domain.model.history.PostHistory
import me.helloc.techwikiplus.post.domain.model.history.PostHistoryId
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.review.PostReviewId
import me.helloc.techwikiplus.post.domain.model.review.PostRevisionId
import me.helloc.techwikiplus.post.domain.service.port.PostHistoryRepository
import org.springframework.stereotype.Repository

@Repository
class PostHistoryRepositoryImpl(
    private val jpaRepository: PostHistoryJpaRepository,
) : PostHistoryRepository {
    override fun save(postHistory: PostHistory): PostHistory {
        val entity = toEntity(postHistory)
        val saved = jpaRepository.save(entity)
        return toDomain(saved)
    }

    override fun findById(id: PostHistoryId): PostHistory? {
        return jpaRepository.findById(id.value).orElse(null)?.let { toDomain(it) }
    }

    override fun findByPostId(postId: PostId): List<PostHistory> {
        return jpaRepository.findByPostIdOrderByChangedAtDesc(postId.value).map { toDomain(it) }
    }

    private fun toEntity(domain: PostHistory): PostHistoryEntity {
        return PostHistoryEntity(
            id = domain.id.value,
            postId = domain.postId.value,
            title = domain.title,
            body = domain.body,
            changeType = domain.changeType.name,
            changedAt = domain.changedAt,
            reviewId = domain.reviewId?.value,
            revisionId = domain.revisionId?.value,
            changedBy = domain.changedBy,
        )
    }

    private fun toDomain(entity: PostHistoryEntity): PostHistory {
        return PostHistory(
            id = PostHistoryId(entity.id),
            postId = PostId(entity.postId),
            title = entity.title,
            body = entity.body,
            changeType = PostChangeType.valueOf(entity.changeType),
            changedAt = entity.changedAt,
            reviewId = entity.reviewId?.let { PostReviewId(it) },
            revisionId = entity.revisionId?.let { PostRevisionId(it) },
            changedBy = entity.changedBy,
        )
    }
}
