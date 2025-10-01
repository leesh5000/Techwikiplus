package me.helloc.techwikiplus.common.infrastructure

import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.post.PostStatus
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

    override fun findAll(
        cursor: PostId?,
        limit: Int,
        excludeDeleted: Boolean,
    ): List<Post> {
        var posts = storage.values.toList()

        // DELETED 상태 제외
        if (excludeDeleted) {
            posts = posts.filter { it.status != PostStatus.DELETED }
        }

        // ID 내림차순 정렬 (Snowflake ID는 큰 값이 최신)
        posts = posts.sortedByDescending { it.id.value }

        // cursor 처리
        if (cursor != null) {
            posts = posts.filter { it.id.value < cursor.value }
        }

        // limit 적용
        return posts.take(limit)
    }

    override fun findAll(
        page: Int,
        size: Int,
        excludeDeleted: Boolean,
    ): List<Post> {
        var posts = storage.values.toList()

        // DELETED 상태 제외
        if (excludeDeleted) {
            posts = posts.filter { it.status != PostStatus.DELETED }
        }

        // ID 내림차순 정렬 (Snowflake ID는 큰 값이 최신)
        posts = posts.sortedByDescending { it.id.value }

        // 페이지네이션 적용 (page는 1부터 시작)
        val startIndex = (page - 1) * size
        return posts.drop(startIndex).take(size)
    }

    override fun countAll(excludeDeleted: Boolean): Long {
        var posts = storage.values.toList()

        // DELETED 상태 제외
        if (excludeDeleted) {
            posts = posts.filter { it.status != PostStatus.DELETED }
        }

        return posts.size.toLong()
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
