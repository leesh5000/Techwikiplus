package me.helloc.techwikiplus.common.infrastructure.persistence.jpa

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.PostTagEntity
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.PostTagEntityId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PostTagJpaRepository : JpaRepository<PostTagEntity, PostTagEntityId> {
    fun findByPostIdOrderByDisplayOrder(postId: Long): List<PostTagEntity>

    fun findByPostIdInOrderByPostIdAscDisplayOrderAsc(postIds: List<Long>): List<PostTagEntity>

    fun deleteByPostId(postId: Long)
}
