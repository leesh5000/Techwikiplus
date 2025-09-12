package me.helloc.techwikiplus.post.domain.model.review

import java.time.Instant

class RevisionVote(
    val id: Long,
    val revisionId: PostRevisionId,
    val voterId: Long?, // nullable for anonymous users
    val votedAt: Instant,
)
