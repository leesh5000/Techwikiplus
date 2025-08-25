package me.helloc.techwikiplus.post.domain.model.post

import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode
import me.helloc.techwikiplus.post.domain.model.tag.PostTag
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import java.time.Instant

class Post(
    val id: PostId,
    val title: PostTitle,
    val body: PostBody,
    val status: PostStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val tags: List<PostTag> = emptyList(),
) {
    init {
        // PostId validation is already done in the PostId value object
        require(tags.size <= MAX_TAGS) {
            "Post cannot have more than $MAX_TAGS tags"
        }
    }

    fun addTag(
        tagName: TagName,
        displayOrder: Int? = null,
    ): Post {
        if (tags.size >= MAX_TAGS) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.TOO_MANY_TAGS,
                params = arrayOf(MAX_TAGS),
            )
        }

        if (tags.any { it.tagName == tagName }) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.DUPLICATE_TAG,
                params = arrayOf(tagName.value),
            )
        }

        val order = displayOrder ?: tags.size
        val newTags = tags + PostTag(tagName, order)
        val sortedTags = newTags.sortedBy { it.displayOrder }

        return copy(tags = sortedTags)
    }

    fun removeTag(tagName: TagName): Post {
        val newTags =
            tags.filterNot { it.tagName == tagName }
                .mapIndexed { index, postTag ->
                    postTag.copy(displayOrder = index)
                }
        return copy(tags = newTags)
    }

    fun replaceTags(newTagNames: List<TagName>): Post {
        if (newTagNames.size > MAX_TAGS) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.TOO_MANY_TAGS,
                params = arrayOf(MAX_TAGS),
            )
        }

        val uniqueTagNames = newTagNames.distinct()
        if (uniqueTagNames.size != newTagNames.size) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.DUPLICATE_TAG,
            )
        }

        val newTags =
            uniqueTagNames.mapIndexed { index, tagName ->
                PostTag(tagName, index)
            }
        return copy(tags = newTags)
    }

    fun reorderTags(orderedTagNames: List<TagName>): Post {
        val currentTagNames = tags.map { it.tagName }.toSet()
        val orderedTagNamesSet = orderedTagNames.toSet()

        if (orderedTagNamesSet != currentTagNames) {
            throw IllegalArgumentException("Tag list mismatch: provided tags do not match current tags")
        }

        val newTags =
            orderedTagNames.mapIndexed { index, tagName ->
                PostTag(tagName, index)
            }
        return copy(tags = newTags)
    }

    fun copy(
        id: PostId = this.id,
        title: PostTitle = this.title,
        body: PostBody = this.body,
        status: PostStatus = this.status,
        createdAt: Instant = this.createdAt,
        updatedAt: Instant = this.updatedAt,
        tags: List<PostTag> = this.tags,
    ): Post {
        return Post(
            id = id,
            title = title,
            body = body,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            tags = tags,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Post) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    fun isDraft(): Boolean {
        return status == PostStatus.DRAFT
    }

    fun isInReview(): Boolean {
        return status == PostStatus.IN_REVIEW
    }

    fun isReviewed(): Boolean {
        return status == PostStatus.REVIEWED
    }

    fun isDeleted(): Boolean {
        return status == PostStatus.DELETED
    }

    fun delete(updatedAt: Instant): Post {
        if (status == PostStatus.DELETED) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.POST_DELETED,
                params = arrayOf(id.value),
            )
        }
        return copy(
            status = PostStatus.DELETED,
            updatedAt = updatedAt,
        )
    }

    override fun toString(): String {
        return "Post(id='$id', title=${title.value}, " +
            "body=${body.value.take(30)}..., status=$status, " +
            "createdAt=$createdAt, updatedAt=$updatedAt)"
    }

    fun submitForReview(updatedAt: Instant): Post {
        if (status != PostStatus.DRAFT) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.INVALID_POST_STATE,
                params = arrayOf("submitForReview", status),
            )
        }
        return copy(
            status = PostStatus.IN_REVIEW,
            updatedAt = updatedAt,
        )
    }

    fun markAsReviewed(updatedAt: Instant): Post {
        if (status != PostStatus.IN_REVIEW) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.INVALID_POST_STATE,
                params = arrayOf("markAsReviewed", status),
            )
        }
        return copy(
            status = PostStatus.REVIEWED,
            updatedAt = updatedAt,
        )
    }

    fun backToDraft(updatedAt: Instant): Post {
        if (status == PostStatus.DRAFT) {
            return this // 이미 초안 상태면 그대로 반환
        }
        return copy(
            status = PostStatus.DRAFT,
            updatedAt = updatedAt,
        )
    }

    fun updateTitle(
        title: PostTitle,
        updatedAt: Instant,
    ): Post {
        if (status == PostStatus.REVIEWED) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.INVALID_POST_STATE,
                params = arrayOf("updateTitle", status),
            )
        }
        return copy(
            title = title,
            updatedAt = updatedAt,
        )
    }

    fun updateBody(
        body: PostBody,
        updatedAt: Instant,
    ): Post {
        if (status == PostStatus.REVIEWED) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.INVALID_POST_STATE,
                params = arrayOf("updateBody", status),
            )
        }
        return copy(
            body = body,
            updatedAt = updatedAt,
        )
    }

    companion object {
        const val MAX_TAGS = 10

        fun create(
            id: PostId,
            title: PostTitle,
            body: PostBody,
            status: PostStatus = PostStatus.DRAFT,
            tags: List<TagName> = emptyList(),
            createdAt: Instant,
            updatedAt: Instant = createdAt,
        ): Post {
            val postTags =
                tags.mapIndexed { index, tagName ->
                    PostTag(tagName, index)
                }

            return Post(
                id = id,
                title = title,
                body = body,
                status = status,
                createdAt = createdAt,
                updatedAt = updatedAt,
                tags = postTags,
            )
        }
    }
}
