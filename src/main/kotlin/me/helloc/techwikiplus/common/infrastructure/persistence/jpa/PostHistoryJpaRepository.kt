package me.helloc.techwikiplus.common.infrastructure.persistence.jpa

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.PostHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PostHistoryJpaRepository : JpaRepository<PostHistoryEntity, Long> {
    fun findByPostIdOrderByChangedAtDesc(postId: Long): List<PostHistoryEntity>
}
