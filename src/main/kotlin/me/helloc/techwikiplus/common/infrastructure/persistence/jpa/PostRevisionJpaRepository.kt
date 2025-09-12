package me.helloc.techwikiplus.common.infrastructure.persistence.jpa

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.PostRevisionEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PostRevisionJpaRepository : JpaRepository<PostRevisionEntity, Long> {
    fun findByReviewId(reviewId: Long): List<PostRevisionEntity>
}
