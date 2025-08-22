package me.helloc.techwikiplus.post.interfaces.web.port

import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostId

interface ReadPostUseCase {
    fun handle(postId: PostId): Post
}
