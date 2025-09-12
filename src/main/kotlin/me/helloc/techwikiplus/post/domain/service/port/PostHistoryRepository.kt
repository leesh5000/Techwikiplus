package me.helloc.techwikiplus.post.domain.service.port

import me.helloc.techwikiplus.post.domain.model.history.PostHistory
import me.helloc.techwikiplus.post.domain.model.history.PostHistoryId
import me.helloc.techwikiplus.post.domain.model.post.PostId

interface PostHistoryRepository {
    fun save(postHistory: PostHistory): PostHistory

    fun findById(id: PostHistoryId): PostHistory?

    fun findByPostId(postId: PostId): List<PostHistory>
}
