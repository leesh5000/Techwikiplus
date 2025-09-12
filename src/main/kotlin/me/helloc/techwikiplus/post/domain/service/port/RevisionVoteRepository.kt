package me.helloc.techwikiplus.post.domain.service.port

import me.helloc.techwikiplus.post.domain.model.review.PostRevisionId
import me.helloc.techwikiplus.post.domain.model.review.RevisionVote

interface RevisionVoteRepository {
    fun save(vote: RevisionVote): RevisionVote

    fun existsByRevisionIdAndVoterId(
        revisionId: PostRevisionId,
        voterId: Long?,
    ): Boolean
}
