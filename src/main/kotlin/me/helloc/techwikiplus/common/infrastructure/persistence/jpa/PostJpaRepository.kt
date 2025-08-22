package me.helloc.techwikiplus.common.infrastructure.persistence.jpa

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.PostEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PostJpaRepository : JpaRepository<PostEntity, Long> {
    /**
     * DELETED 상태가 아닌 게시글을 ID로 조회
     */
    @Query("SELECT p FROM PostEntity p WHERE p.id = :id AND p.status != 'DELETED'")
    fun findByIdAndNotDeleted(
        @Param("id") id: Long,
    ): Optional<PostEntity>

    /**
     * DELETED 상태가 아닌 게시글의 존재 여부 확인
     */
    @Query("SELECT COUNT(p) > 0 FROM PostEntity p WHERE p.id = :id AND p.status != 'DELETED'")
    fun existsByIdAndNotDeleted(
        @Param("id") id: Long,
    ): Boolean
}
