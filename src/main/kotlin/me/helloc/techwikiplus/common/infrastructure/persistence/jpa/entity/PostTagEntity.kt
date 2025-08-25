package me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant

class PostTagEntityId : Serializable {
    var postId: Long = 0L
    var tagId: Long = 0L

    constructor()

    constructor(postId: Long, tagId: Long) {
        this.postId = postId
        this.tagId = tagId
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PostTagEntityId) return false
        return postId == other.postId && tagId == other.tagId
    }

    override fun hashCode(): Int {
        var result = postId.hashCode()
        result = 31 * result + tagId.hashCode()
        return result
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

@Entity
@Table(name = "post_tags")
@IdClass(PostTagEntityId::class)
open class PostTagEntity(
    @Id
    @Column(name = "post_id", nullable = false, columnDefinition = "BIGINT")
    open val postId: Long,
    @Id
    @Column(name = "tag_id", nullable = false, columnDefinition = "BIGINT")
    open val tagId: Long,
    @Column(name = "display_order", nullable = false)
    open val displayOrder: Int = 0,
    @Column(name = "created_at", nullable = false)
    open val createdAt: Instant,
) {
    // JPA requires a no-arg constructor
    protected constructor() : this(
        postId = 0L,
        tagId = 0L,
        displayOrder = 0,
        createdAt = Instant.now(),
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PostTagEntity) return false

        return postId == other.postId && tagId == other.tagId
    }

    override fun hashCode(): Int {
        var result = postId.hashCode()
        result = 31 * result + tagId.hashCode()
        return result
    }

    override fun toString(): String {
        return "PostTagEntity(postId=$postId, tagId=$tagId, " +
            "displayOrder=$displayOrder, createdAt=$createdAt)"
    }
}
