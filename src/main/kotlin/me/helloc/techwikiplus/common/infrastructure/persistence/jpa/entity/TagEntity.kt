package me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "tags")
open class TagEntity(
    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BIGINT")
    open val id: Long,
    @Column(name = "name", nullable = false, length = 30, unique = true)
    open val name: String,
    @Column(name = "post_count", nullable = false)
    open val postCount: Int = 0,
    @Column(name = "created_at", nullable = false)
    open val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    open val updatedAt: Instant,
) {
    // JPA requires a no-arg constructor
    protected constructor() : this(
        id = 0L,
        name = "",
        postCount = 0,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TagEntity) return false

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "TagEntity(id='$id', name='$name', " +
            "postCount=$postCount, createdAt=$createdAt, updatedAt=$updatedAt)"
    }
}
