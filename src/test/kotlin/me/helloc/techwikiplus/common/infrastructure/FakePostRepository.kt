package me.helloc.techwikiplus.common.infrastructure

import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.service.port.PostRepository

class FakePostRepository : PostRepository {
    private val storage = mutableMapOf<PostId, Post>()

    override fun findBy(id: PostId): Post? {
        return storage[id]
    }

    override fun existsBy(id: PostId): Boolean {
        return storage.containsKey(id)
    }

    override fun save(post: Post): Post {
        storage[post.id] = post
        return post
    }

    fun getAll(): List<Post> {
        return storage.values.toList()
    }

    fun findAll(): List<Post> {
        return storage.values.toList()
    }

    fun clear() {
        storage.clear()
    }

    fun count(): Int {
        return storage.size
    }
}
