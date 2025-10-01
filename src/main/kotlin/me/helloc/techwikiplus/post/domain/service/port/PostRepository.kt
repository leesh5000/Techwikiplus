package me.helloc.techwikiplus.post.domain.service.port

import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostId

interface PostRepository {
    fun findBy(id: PostId): Post?

    fun existsBy(id: PostId): Boolean

    fun save(post: Post): Post

    fun findAll(
        cursor: PostId? = null,
        limit: Int = 20,
        excludeDeleted: Boolean = true,
    ): List<Post>

    fun findAll(
        page: Int,
        size: Int,
        excludeDeleted: Boolean = true,
    ): List<Post>

    fun countAll(excludeDeleted: Boolean = true): Long
}
