package me.helloc.techwikiplus.post.domain.model.tag

import java.time.Instant

class Tag(
    val id: TagId,
    val name: TagName,
    val postCount: Int = 0,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(postCount >= 0) { "Post count cannot be negative" }
    }

    fun incrementPostCount(): Tag {
        return copy(postCount = postCount + 1)
    }

    fun decrementPostCount(): Tag {
        return if (postCount > 0) {
            copy(postCount = postCount - 1)
        } else {
            this
        }
    }

    fun copy(
        id: TagId = this.id,
        name: TagName = this.name,
        postCount: Int = this.postCount,
        createdAt: Instant = this.createdAt,
        updatedAt: Instant = this.updatedAt,
    ): Tag {
        return Tag(
            id = id,
            name = name,
            postCount = postCount,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Tag) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Tag(id=$id, name=${name.value}, postCount=$postCount)"
    }

    companion object {
        fun create(
            id: TagId,
            name: TagName,
            now: Instant,
        ): Tag {
            return Tag(
                id = id,
                name = name,
                postCount = 0,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}