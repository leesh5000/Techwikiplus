package me.helloc.techwikiplus.common.infrastructure.persistence

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.PostRevisionJpaRepository
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.PostRevisionEntity
import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.review.PostReviewId
import me.helloc.techwikiplus.post.domain.model.review.PostRevision
import me.helloc.techwikiplus.post.domain.model.review.PostRevisionId
import me.helloc.techwikiplus.post.domain.service.port.PostRevisionRepository
import org.springframework.stereotype.Repository

@Repository
class PostRevisionRepositoryImpl(
    private val jpaRepository: PostRevisionJpaRepository,
) : PostRevisionRepository {
    override fun save(postRevision: PostRevision): PostRevision {
        val entity = toEntity(postRevision)
        val saved = jpaRepository.save(entity)
        return toDomain(saved)
    }

    override fun findById(id: PostRevisionId): PostRevision? {
        return jpaRepository.findById(id.value).orElse(null)?.let { toDomain(it) }
    }

    override fun findByReviewId(reviewId: PostReviewId): List<PostRevision> {
        return jpaRepository.findByReviewId(reviewId.value).map { toDomain(it) }
    }

    private fun toEntity(domain: PostRevision): PostRevisionEntity {
        return PostRevisionEntity(
            id = domain.id.value,
            reviewId = domain.reviewId.value,
            authorId = domain.authorId,
            title = domain.title.value,
            body = domain.body.value,
            submittedAt = domain.submittedAt,
            voteCount = domain.voteCount,
        )
    }

    private fun toDomain(entity: PostRevisionEntity): PostRevision {
        return PostRevision(
            id = PostRevisionId(entity.id),
            reviewId = PostReviewId(entity.reviewId),
            authorId = entity.authorId,
            title = PostTitle(entity.title),
            body = PostBody(entity.body),
            submittedAt = entity.submittedAt,
            voteCount = entity.voteCount,
        )
    }
}
