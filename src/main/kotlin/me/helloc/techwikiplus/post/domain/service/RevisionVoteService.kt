package me.helloc.techwikiplus.post.domain.service

import me.helloc.techwikiplus.common.domain.service.port.ClockHolder
import me.helloc.techwikiplus.common.infrastructure.id.Snowflake
import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode
import me.helloc.techwikiplus.post.domain.model.review.PostRevisionId
import me.helloc.techwikiplus.post.domain.model.review.RevisionVote
import me.helloc.techwikiplus.post.domain.service.port.PostRevisionRepository
import me.helloc.techwikiplus.post.domain.service.port.RevisionVoteRepository
import me.helloc.techwikiplus.user.domain.service.port.LockManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Service
@Transactional
class RevisionVoteService(
    private val revisionVoteRepository: RevisionVoteRepository,
    private val postRevisionRepository: PostRevisionRepository,
    private val lockManager: LockManager,
    private val clockHolder: ClockHolder,
    private val snowflake: Snowflake,
) {
    fun vote(
        revisionId: PostRevisionId,
        voterId: Long? = null,
    ) {
        // 비로그인 사용자는 투표할 수 없음 (간단한 구현)
        if (voterId == null) {
            return
        }

        val lockKey = "vote:revision:${revisionId.value}:voter:$voterId"

        lockManager.executeWithLock(
            key = lockKey,
            waitTime = Duration.ofSeconds(5),
            leaseTime = Duration.ofSeconds(10),
        ) {
            // 중복 투표 체크
            if (revisionVoteRepository.existsByRevisionIdAndVoterId(revisionId, voterId)) {
                throw PostDomainException(
                    postErrorCode = PostErrorCode.ALREADY_VOTED,
                    params = arrayOf(revisionId.value),
                )
            }

            // 투표 저장
            val vote =
                RevisionVote(
                    // Repository에서 생성
                    id = 0L,
                    revisionId = revisionId,
                    voterId = voterId,
                    votedAt = clockHolder.now(),
                )
            revisionVoteRepository.save(vote)

            // vote_count 업데이트
            val revision =
                postRevisionRepository.findById(revisionId)
                    ?: throw PostDomainException(
                        postErrorCode = PostErrorCode.REVISION_NOT_FOUND,
                        params = arrayOf(revisionId.value),
                    )

            val updatedRevision = revision.incrementVoteCount()
            postRevisionRepository.save(updatedRevision)
        }
    }
}
