package me.helloc.techwikiplus.post.domain.service

import me.helloc.techwikiplus.common.domain.service.port.ClockHolder
import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode
import me.helloc.techwikiplus.post.domain.model.history.PostChangeType
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.review.PostReview
import me.helloc.techwikiplus.post.domain.model.review.PostReviewId
import me.helloc.techwikiplus.post.domain.model.review.PostRevision
import me.helloc.techwikiplus.post.domain.service.port.PostRepository
import me.helloc.techwikiplus.post.domain.service.port.PostReviewIdGenerator
import me.helloc.techwikiplus.post.domain.service.port.PostReviewRepository
import me.helloc.techwikiplus.post.domain.service.port.PostRevisionRepository
import me.helloc.techwikiplus.user.domain.service.port.LockManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
@Transactional
class PostReviewService(
    private val postReviewRepository: PostReviewRepository,
    private val postRevisionRepository: PostRevisionRepository,
    private val postRepository: PostRepository,
    private val postReviewIdGenerator: PostReviewIdGenerator,
    private val clockHolder: ClockHolder,
    private val lockManager: LockManager,
    private val postHistoryService: PostHistoryService,
) {
    fun startReview(
        postId: PostId,
        startedBy: Long? = null,
    ): PostReview {
        // 게시글 존재 여부 확인
        val post =
            postRepository.findBy(postId)
                ?: throw PostDomainException(
                    postErrorCode = PostErrorCode.POST_NOT_FOUND,
                    params = arrayOf(postId.value),
                )

        // 삭제된 게시글은 검수를 시작할 수 없음
        if (post.status == me.helloc.techwikiplus.post.domain.model.post.PostStatus.DELETED) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.POST_NOT_FOUND,
                params = arrayOf(postId.value),
            )
        }

        // 이미 진행 중인 리뷰가 있는지 확인
        postReviewRepository.findByPostId(postId)?.let {
            throw PostDomainException(
                postErrorCode = PostErrorCode.REVIEW_ALREADY_EXISTS,
                params = arrayOf(postId.value),
            )
        }

        val review =
            PostReview.create(
                id = postReviewIdGenerator.generate(),
                postId = postId,
                now = clockHolder.now(),
                startedBy = startedBy,
            )

        return postReviewRepository.save(review)
    }

    fun completeReview(reviewId: PostReviewId) {
        val lockKey = "review:complete:${reviewId.value}"

        lockManager.executeWithLock(
            key = lockKey,
            waitTime = Duration.ofSeconds(10),
            leaseTime = Duration.ofSeconds(30),
        ) {
            val review =
                postReviewRepository.findById(reviewId)
                    ?: throw PostDomainException(
                        postErrorCode = PostErrorCode.REVIEW_NOT_FOUND,
                        params = arrayOf(reviewId.value),
                    )

            // 이미 완료된 리뷰인지 확인
            if (review.status != me.helloc.techwikiplus.post.domain.model.review.PostReviewStatus.IN_REVIEW) {
                return@executeWithLock
            }

            // 수정본 목록 가져오기
            val revisions = postRevisionRepository.findByReviewId(reviewId)

            if (revisions.isEmpty()) {
                // 수정본이 없으면 리뷰 취소
                val cancelledReview =
                    PostReview(
                        id = review.id,
                        postId = review.postId,
                        startedAt = review.startedAt,
                        deadline = review.deadline,
                        status = me.helloc.techwikiplus.post.domain.model.review.PostReviewStatus.CANCELLED,
                        winningRevisionId = null,
                        startedBy = review.startedBy,
                    )
                postReviewRepository.save(cancelledReview)
                return@executeWithLock
            }

            // 최다 득표 수정본 선정 (동점일 경우 먼저 제출된 것)
            val winningRevision =
                revisions
                    .sortedWith(
                        compareByDescending<PostRevision> { it.voteCount }
                            .thenBy { it.submittedAt },
                    )
                    .first()

            // 리뷰 완료 처리
            val completedReview = review.complete(winningRevision.id)
            postReviewRepository.save(completedReview)

            // Post 업데이트
            val post =
                postRepository.findBy(review.postId)
                    ?: throw PostDomainException(
                        postErrorCode = PostErrorCode.POST_NOT_FOUND,
                        params = arrayOf(review.postId.value),
                    )

            val updatedPost =
                post.copy(
                    title = winningRevision.title,
                    body = winningRevision.body,
                    status = me.helloc.techwikiplus.post.domain.model.post.PostStatus.REVIEWED,
                    updatedAt = clockHolder.now(),
                )
            postRepository.save(updatedPost)

            // 변경 이력 저장
            postHistoryService.saveHistory(
                post = updatedPost,
                changeType = PostChangeType.REVIEWED,
                reviewId = review.id,
                revisionId = winningRevision.id,
                changedBy = winningRevision.authorId,
            )
        }
    }

    @Transactional(readOnly = true)
    fun getReview(postId: PostId): PostReview? {
        return postReviewRepository.findByPostId(postId)
    }

    @Transactional(readOnly = true)
    fun getById(reviewId: PostReviewId): PostReview {
        return postReviewRepository.findById(reviewId)
            ?: throw PostDomainException(
                postErrorCode = PostErrorCode.REVIEW_NOT_FOUND,
                params = arrayOf(reviewId.value),
            )
    }

    @Transactional(readOnly = true)
    fun getExpiredReviews(): List<PostReview> {
        return postReviewRepository.findExpiredReviews(clockHolder.now())
    }

    @Transactional(readOnly = true)
    fun getReviewsByPostId(postId: PostId): List<PostReview> {
        return postReviewRepository.findAllByPostId(postId)
    }
}
