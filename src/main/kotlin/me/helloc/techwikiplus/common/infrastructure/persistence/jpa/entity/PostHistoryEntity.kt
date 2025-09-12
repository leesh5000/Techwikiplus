package me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "post_histories")
class PostHistoryEntity(
    @Id
    val id: Long,
    @Column(name = "post_id", nullable = false)
    val postId: Long,
    @Column(nullable = false)
    val title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    val body: String,
    @Column(name = "changed_at", nullable = false)
    val changedAt: Instant,
    @Column(name = "review_id")
    val reviewId: Long? = null,
    @Column(name = "revision_id")
    val revisionId: Long? = null,
    @Column(name = "changed_by")
    val changedBy: Long? = null,
)
