package me.helloc.techwikiplus.post.domain.service

import me.helloc.techwikiplus.common.domain.service.port.ClockHolder
import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode
import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.review.PostReviewId
import me.helloc.techwikiplus.post.domain.model.review.PostRevision
import me.helloc.techwikiplus.post.domain.model.review.PostRevisionId
import me.helloc.techwikiplus.post.domain.model.review.ReviewComment
import me.helloc.techwikiplus.post.domain.model.review.ReviewCommentType
import me.helloc.techwikiplus.post.domain.service.port.PostRepository
import me.helloc.techwikiplus.post.domain.service.port.PostReviewRepository
import me.helloc.techwikiplus.post.domain.service.port.PostRevisionIdGenerator
import me.helloc.techwikiplus.post.domain.service.port.PostRevisionRepository
import me.helloc.techwikiplus.post.domain.service.port.ReviewCommentIdGenerator
import me.helloc.techwikiplus.post.dto.request.PostRevisionRequest
import me.helloc.techwikiplus.post.dto.response.PostRevisionResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PostRevisionService(
    private val postRevisionRepository: PostRevisionRepository,
    private val postReviewRepository: PostReviewRepository,
    private val postRepository: PostRepository,
    private val postRevisionIdGenerator: PostRevisionIdGenerator,
    private val reviewCommentIdGenerator: ReviewCommentIdGenerator,
    private val clockHolder: ClockHolder,
) {
    fun submitRevision(
        reviewId: PostReviewId,
        request: PostRevisionRequest,
        authorId: Long? = null,
    ): PostRevision {
        // Validate that total length of all suggestedChanges does not exceed PostBody MAX_LENGTH
        val totalSuggestedChangeLength = request.reviewComments.sumOf { it.suggestedChange.length }
        if (totalSuggestedChangeLength > POST_BODY_MAX_LENGTH) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.TOTAL_SUGGESTED_CHANGES_TOO_LONG,
                params = arrayOf(totalSuggestedChangeLength, POST_BODY_MAX_LENGTH),
            )
        }

        // Convert request DTOs to domain models
        val reviewComments =
            request.reviewComments.map { commentRequest ->
                ReviewComment(
                    id = reviewCommentIdGenerator.next(),
                    lineNumber = commentRequest.lineNumber,
                    comment = commentRequest.comment,
                    type = ReviewCommentType.valueOf(commentRequest.type),
                    suggestedChange = commentRequest.suggestedChange,
                )
            }

        val revision =
            PostRevision(
                id = postRevisionIdGenerator.generate(),
                reviewId = reviewId,
                authorId = authorId,
                title = PostTitle(request.title),
                body = PostBody(request.body),
                reviewComments = reviewComments,
                submittedAt = clockHolder.now(),
                voteCount = 0,
            )

        return postRevisionRepository.save(revision)
    }

    companion object {
        // Same as PostBody.MAX_LENGTH
        private const val POST_BODY_MAX_LENGTH = 50000
    }

    @Transactional(readOnly = true)
    fun getRevisions(reviewId: PostReviewId): List<PostRevision> {
        return postRevisionRepository.findByReviewId(reviewId)
    }

    @Transactional(readOnly = true)
    fun getById(revisionId: PostRevisionId): PostRevision {
        return postRevisionRepository.findById(revisionId)
            ?: throw PostDomainException(
                postErrorCode = PostErrorCode.REVISION_NOT_FOUND,
                params = arrayOf(revisionId.value),
            )
    }

    @Transactional(readOnly = true)
    fun getRevisionByRevisionId(revisionId: PostRevisionId): PostRevisionResponse {
        val revision =
            postRevisionRepository.findById(revisionId)
                ?: throw PostDomainException(
                    postErrorCode = PostErrorCode.REVISION_NOT_FOUND,
                    params = arrayOf(revisionId.value),
                )

        val review =
            postReviewRepository.findById(revision.reviewId)
                ?: throw PostDomainException(
                    postErrorCode = PostErrorCode.REVIEW_NOT_FOUND,
                    params = arrayOf(revision.reviewId.value),
                )

        val originalPost =
            postRepository.findBy(review.postId)
                ?: throw PostDomainException(
                    postErrorCode = PostErrorCode.POST_NOT_FOUND,
                    params = arrayOf(review.postId.value),
                )

        return PostRevisionResponse.from(revision, originalPost)
    }

    @Transactional(readOnly = true)
    fun getRevisionByReviewId(reviewId: PostReviewId): List<PostRevisionResponse> {
        val revisions = postRevisionRepository.findByReviewId(reviewId)
        if (revisions.isEmpty()) {
            return emptyList()
        }

        val review =
            postReviewRepository.findById(reviewId)
                ?: throw PostDomainException(
                    postErrorCode = PostErrorCode.REVIEW_NOT_FOUND,
                    params = arrayOf(reviewId.value),
                )

        val originalPost =
            postRepository.findBy(review.postId)
                ?: throw PostDomainException(
                    postErrorCode = PostErrorCode.POST_NOT_FOUND,
                    params = arrayOf(review.postId.value),
                )

        return revisions.map { PostRevisionResponse.from(it, originalPost) }
    }
}
