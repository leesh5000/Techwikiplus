package me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "review_comments")
open class ReviewCommentEntity(
    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BIGINT")
    open val id: Long,
    @Column(name = "revision_id", nullable = false)
    open val revisionId: Long,
    @Column(name = "line_number", nullable = false)
    open val lineNumber: Int,
    @Column(name = "comment", nullable = false, columnDefinition = "TEXT")
    open val comment: String,
    @Column(name = "comment_type", nullable = false, length = 50)
    open val commentType: String,
    @Column(name = "created_at", nullable = false)
    open val createdAt: Instant,
) {
    // JPA requires a no-arg constructor
    protected constructor() : this(
        id = 0L,
        revisionId = 0L,
        lineNumber = 0,
        comment = "",
        commentType = "GENERAL",
        createdAt = Instant.now(),
    )
}
