package me.helloc.techwikiplus.post.interfaces.web.port

import me.helloc.techwikiplus.post.domain.model.PostBody
import me.helloc.techwikiplus.post.domain.model.PostId
import me.helloc.techwikiplus.post.domain.model.PostTitle

interface CreatePostUseCase {
    fun handle(
        title: PostTitle,
        body: PostBody,
    ): PostId
}
