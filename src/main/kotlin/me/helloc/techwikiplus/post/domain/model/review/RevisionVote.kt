package me.helloc.techwikiplus.post.domain.model.review

import java.time.Instant

class RevisionVote(
    val id: Long,
    val revisionId: PostRevisionId,
    // nullable for anonymous users
    val voterId: Long?,
    val votedAt: Instant,
)
