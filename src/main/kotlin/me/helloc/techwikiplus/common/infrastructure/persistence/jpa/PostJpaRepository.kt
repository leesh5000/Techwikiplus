package me.helloc.techwikiplus.common.infrastructure.persistence.jpa

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.PostEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PostJpaRepository : JpaRepository<PostEntity, Long> {
    fun findByIdLessThanAndStatusNotOrderByIdDesc(
        id: Long,
        status: String,
        pageable: Pageable,
    ): List<PostEntity>

    fun findByStatusNotOrderByIdDesc(
        status: String,
        pageable: Pageable,
    ): List<PostEntity>

    fun countByStatusNot(status: String): Long
}
