package me.helloc.techwikiplus.post.interfaces.web.port

import me.helloc.techwikiplus.post.domain.model.post.PostId

interface DeletePostUseCase {
    fun handle(postId: PostId)
}
