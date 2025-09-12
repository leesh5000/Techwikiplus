package me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "post_revisions")
open class PostRevisionEntity(
    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BIGINT")
    open val id: Long,
    @Column(name = "review_id", nullable = false)
    open val reviewId: Long,
    @Column(name = "author_id")
    open val authorId: Long? = null,
    @Column(name = "title", nullable = false, length = 200)
    open val title: String,
    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    open val body: String,
    @Column(name = "submitted_at", nullable = false)
    open val submittedAt: Instant,
    @Column(name = "vote_count", nullable = false)
    open val voteCount: Int = 0,
) {
    // JPA requires a no-arg constructor
    protected constructor() : this(
        id = 0L,
        reviewId = 0L,
        authorId = null,
        title = "",
        body = "",
        submittedAt = Instant.now(),
        voteCount = 0,
    )
}
