package me.helloc.techwikiplus.post.domain.model.post

import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode
import me.helloc.techwikiplus.post.domain.model.tag.PostTag
import java.time.Instant

class Post(
    val id: PostId,
    val title: PostTitle,
    val body: PostBody,
    val status: PostStatus,
    val version: PostRevisionVersion,
    val tags: Set<PostTag> = emptySet(),
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        // PostId validation is already done in the PostId value object
        require(tags.size <= MAX_TAGS) {
            "Post cannot have more than $MAX_TAGS tags"
        }
    }

    fun copy(
        id: PostId = this.id,
        title: PostTitle = this.title,
        body: PostBody = this.body,
        status: PostStatus = this.status,
        version: PostRevisionVersion = this.version,
        tags: Set<PostTag> = this.tags,
        createdAt: Instant = this.createdAt,
        updatedAt: Instant = this.updatedAt,
    ): Post {
        return Post(
            id = id,
            title = title,
            body = body,
            status = status,
            version = version,
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

    fun delete(updatedAt: Instant): Post {
        if (status == PostStatus.DELETED) {
            throw PostDomainException(
                postErrorCode = PostErrorCode.POST_DELETED,
                params = arrayOf(id),
            )
        }
        return copy(
            status = PostStatus.DELETED,
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
            version: PostRevisionVersion = PostRevisionVersion(),
            postTags: Set<PostTag> = emptySet(),
            createdAt: Instant,
            updatedAt: Instant = createdAt,
        ): Post {
            return Post(
                id = id,
                title = title,
                body = body,
                status = status,
                version = version,
                tags = postTags,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
        }
    }
}
