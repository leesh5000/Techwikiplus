package me.helloc.techwikiplus.post.domain.service.port

import me.helloc.techwikiplus.post.domain.model.Post
import me.helloc.techwikiplus.post.domain.model.PostId

interface PostRepository {
    fun save(post: Post): Post

    fun findBy(id: PostId): Post?

    fun existsBy(id: PostId): Boolean
}
