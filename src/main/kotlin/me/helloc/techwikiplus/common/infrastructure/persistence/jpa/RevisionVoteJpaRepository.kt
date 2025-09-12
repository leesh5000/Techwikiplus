package me.helloc.techwikiplus.common.infrastructure.persistence.jpa

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.RevisionVoteEntity
import org.springframework.data.jpa.repository.JpaRepository

interface RevisionVoteJpaRepository : JpaRepository<RevisionVoteEntity, Long> {
    fun existsByRevisionIdAndVoterId(
        revisionId: Long,
        voterId: Long?,
    ): Boolean
}
