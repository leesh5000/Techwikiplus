package me.helloc.techwikiplus.common.infrastructure.persistence

import me.helloc.techwikiplus.common.infrastructure.id.Snowflake
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.RevisionVoteJpaRepository
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.RevisionVoteEntity
import me.helloc.techwikiplus.post.domain.model.review.PostRevisionId
import me.helloc.techwikiplus.post.domain.model.review.RevisionVote
import me.helloc.techwikiplus.post.domain.service.port.RevisionVoteRepository
import org.springframework.stereotype.Repository

@Repository
class RevisionVoteRepositoryImpl(
    private val jpaRepository: RevisionVoteJpaRepository,
    private val snowflake: Snowflake,
) : RevisionVoteRepository {
    override fun save(vote: RevisionVote): RevisionVote {
        val entity = toEntity(vote)
        val saved = jpaRepository.save(entity)
        return toDomain(saved)
    }

    override fun existsByRevisionIdAndVoterId(
        revisionId: PostRevisionId,
        voterId: Long?,
    ): Boolean {
        return jpaRepository.existsByRevisionIdAndVoterId(revisionId.value, voterId)
    }

    private fun toEntity(domain: RevisionVote): RevisionVoteEntity {
        return RevisionVoteEntity(
            id = if (domain.id == 0L) snowflake.nextId() else domain.id,
            revisionId = domain.revisionId.value,
            voterId = domain.voterId,
            votedAt = domain.votedAt,
        )
    }

    private fun toDomain(entity: RevisionVoteEntity): RevisionVote {
        return RevisionVote(
            id = entity.id,
            revisionId = PostRevisionId(entity.revisionId),
            voterId = entity.voterId,
            votedAt = entity.votedAt,
        )
    }
}
