package me.helloc.techwikiplus.post.domain.service.port

import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.post.PostStatus
import me.helloc.techwikiplus.post.domain.model.tag.TagName

interface PostRepository {
    fun save(post: Post): Post

    fun findBy(id: PostId): Post?

    fun findBy(
        id: PostId,
        excludedStatuses: Set<PostStatus> = emptySet(),
    ): Post?

    fun existsBy(id: PostId): Boolean

    fun existsBy(
        id: PostId,
        excludedStatuses: Set<PostStatus> = emptySet(),
    ): Boolean

    fun findByTag(
        tagName: TagName,
        offset: Int,
        limit: Int,
    ): List<Post>
}
