package me.helloc.techwikiplus.common.infrastructure.persistence.jpa

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.TagEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface TagJpaRepository : JpaRepository<TagEntity, Long> {
    fun findByName(name: String): Optional<TagEntity>

    fun findByNameIn(names: List<String>): List<TagEntity>

    @Modifying
    @Query("UPDATE TagEntity t SET t.postCount = t.postCount + 1 WHERE t.id = :tagId")
    fun incrementPostCount(
        @Param("tagId") tagId: Long,
    )

    @Modifying
    @Query("UPDATE TagEntity t SET t.postCount = CASE WHEN t.postCount > 0 THEN t.postCount - 1 ELSE 0 END WHERE t.id = :tagId")
    fun decrementPostCount(
        @Param("tagId") tagId: Long,
    )
}
