package me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "posts",
)
open class PostEntity(
    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BIGINT")
    open val id: Long,
    @Column(name = "title", nullable = false, length = 200)
    open val title: String,
    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    open val body: String,
    @Column(name = "status", nullable = false, length = 20)
    open val status: String = "DRAFT",
    @Column(name = "version", nullable = false)
    open val version: Long = 0L,
    @Column(name = "created_at", nullable = false)
    open val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    open val updatedAt: Instant,
) {
    // JPA requires a no-arg constructor
    protected constructor() : this(
        id = 0L,
        title = "",
        body = "",
        status = "DRAFT",
        version = 0L,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PostEntity) return false

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "PostEntity(id='$id', title='$title', " +
            "status='$status', createdAt=$createdAt, updatedAt=$updatedAt)"
    }
}
