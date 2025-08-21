package me.helloc.techwikiplus.post.interfaces.web.port

import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.post.PostTitle

interface CreatePostUseCase {
    fun handle(
        title: PostTitle,
        body: PostBody,
    ): PostId
}
