package me.helloc.techwikiplus.common.infrastructure.persistence

import me.helloc.techwikiplus.common.domain.service.port.ClockHolder
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.PostRevisionJpaRepository
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.PostRevisionEntity
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.ReviewCommentEntity
import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.review.PostReviewId
import me.helloc.techwikiplus.post.domain.model.review.PostRevision
import me.helloc.techwikiplus.post.domain.model.review.PostRevisionId
import me.helloc.techwikiplus.post.domain.model.review.ReviewComment
import me.helloc.techwikiplus.post.domain.model.review.ReviewCommentId
import me.helloc.techwikiplus.post.domain.model.review.ReviewCommentType
import me.helloc.techwikiplus.post.domain.service.port.PostRevisionRepository
import me.helloc.techwikiplus.post.domain.service.port.ReviewCommentIdGenerator
import org.springframework.stereotype.Repository

@Repository
class PostRevisionRepositoryImpl(
    private val jpaRepository: PostRevisionJpaRepository,
    private val reviewCommentIdGenerator: ReviewCommentIdGenerator,
    private val clockHolder: ClockHolder,
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
        val entity =
            PostRevisionEntity(
                id = domain.id.value,
                reviewId = domain.reviewId.value,
                authorId = domain.authorId,
                title = domain.title.value,
                body = domain.body.value,
                submittedAt = domain.submittedAt,
                voteCount = domain.voteCount,
            )

        // Add review comments to entity
        domain.reviewComments.forEach { comment ->
            entity.reviewComments.add(
                ReviewCommentEntity(
                    id = comment.id.value,
                    revisionId = domain.id.value,
                    lineNumber = comment.lineNumber,
                    comment = comment.comment,
                    commentType = comment.type.name,
                    suggestedChange = comment.suggestedChange,
                    createdAt = clockHolder.now(),
                ),
            )
        }

        return entity
    }

    private fun toDomain(entity: PostRevisionEntity): PostRevision {
        return PostRevision(
            id = PostRevisionId(entity.id),
            reviewId = PostReviewId(entity.reviewId),
            authorId = entity.authorId,
            title = PostTitle(entity.title),
            body = PostBody(entity.body),
            reviewComments =
                entity.reviewComments.map { commentEntity ->
                    ReviewComment(
                        id = ReviewCommentId(commentEntity.id),
                        lineNumber = commentEntity.lineNumber,
                        comment = commentEntity.comment,
                        type = ReviewCommentType.valueOf(commentEntity.commentType),
                        suggestedChange = commentEntity.suggestedChange,
                    )
                },
            submittedAt = entity.submittedAt,
            voteCount = entity.voteCount,
        )
    }
}
