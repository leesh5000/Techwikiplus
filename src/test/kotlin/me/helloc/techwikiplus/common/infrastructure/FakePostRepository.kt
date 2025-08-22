package me.helloc.techwikiplus.common.infrastructure

import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.post.PostStatus
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import me.helloc.techwikiplus.post.domain.service.port.PostRepository

class FakePostRepository : PostRepository {
    private val storage = mutableMapOf<PostId, Post>()

    override fun findBy(id: PostId): Post? {
        return findBy(id, setOf(PostStatus.DELETED))
    }

    override fun findBy(
        id: PostId,
        excludedStatuses: Set<PostStatus>,
    ): Post? {
        val post = storage[id]
        return if (post != null && excludedStatuses.none { it == post.status }) {
            post
        } else {
            null
        }
    }

    override fun existsBy(id: PostId): Boolean {
        return existsBy(id, setOf(PostStatus.DELETED))
    }

    override fun existsBy(
        id: PostId,
        excludedStatuses: Set<PostStatus>,
    ): Boolean {
        val post = storage[id]
        return post != null && excludedStatuses.none { it == post.status }
    }

    override fun findByTag(
        tagName: TagName,
        offset: Int,
        limit: Int,
    ): List<Post> {
        return storage.values.filter { post ->
            post.tags.any { it.tagName == tagName }
        }.sortedByDescending { it.createdAt }
            .drop(offset)
            .take(limit)
    }

    override fun save(post: Post): Post {
        storage[post.id] = post
        return post
    }

    fun getAll(): List<Post> {
        return storage.values.toList()
    }

    fun clear() {
        storage.clear()
    }

    fun count(): Int {
        return storage.size
    }
}
