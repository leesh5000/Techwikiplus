package me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "revision_votes")
open class RevisionVoteEntity(
    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BIGINT")
    open val id: Long,
    @Column(name = "revision_id", nullable = false)
    open val revisionId: Long,
    @Column(name = "voter_id")
    open val voterId: Long? = null,
    @Column(name = "voted_at", nullable = false)
    open val votedAt: Instant,
) {
    // JPA requires a no-arg constructor
    protected constructor() : this(
        id = 0L,
        revisionId = 0L,
        voterId = null,
        votedAt = Instant.now(),
    )
}
