package me.helloc.techwikiplus.post.application.scheduler

import me.helloc.techwikiplus.post.domain.service.PostReviewService
import me.helloc.techwikiplus.user.domain.service.port.LockManager
import me.helloc.techwikiplus.user.domain.service.port.LockManagerException
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ReviewDeadlineScheduler(
    private val postReviewService: PostReviewService,
    private val lockManager: LockManager,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ReviewDeadlineScheduler::class.java)
    }

    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    fun processExpiredReviews() {
        try {
            val expiredReviews = postReviewService.getExpiredReviews()

            expiredReviews.forEach { review ->
                val lockKey = "review:complete:${review.id.value}"

                try {
                    lockManager.executeWithLock(
                        key = lockKey,
                        waitTime = Duration.ofSeconds(0), // 즉시 획득 시도
                        leaseTime = Duration.ofSeconds(30),
                    ) {
                        postReviewService.completeReview(review.id)
                        logger.info("Completed expired review: ${review.id.value}")
                    }
                } catch (e: LockManagerException) {
                    // 다른 인스턴스에서 처리 중
                    logger.debug("Review ${review.id.value} is being processed by another instance")
                } catch (e: Exception) {
                    logger.error("Failed to complete review ${review.id.value}", e)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to process expired reviews", e)
        }
    }
}
