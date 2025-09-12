package me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "post_reviews")
open class PostReviewEntity(
    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BIGINT")
    open val id: Long,
    @Column(name = "post_id", nullable = false)
    open val postId: Long,
    @Column(name = "started_at", nullable = false)
    open val startedAt: Instant,
    @Column(name = "deadline", nullable = false)
    open val deadline: Instant,
    @Column(name = "status", nullable = false, length = 20)
    open val status: String,
    @Column(name = "winning_revision_id")
    open val winningRevisionId: Long? = null,
    @Column(name = "started_by")
    open val startedBy: Long? = null,
) {
    // JPA requires a no-arg constructor
    protected constructor() : this(
        id = 0L,
        postId = 0L,
        startedAt = Instant.now(),
        deadline = Instant.now(),
        status = "IN_REVIEW",
        winningRevisionId = null,
        startedBy = null,
    )
}
