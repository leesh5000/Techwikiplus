package me.helloc.techwikiplus.post.interfaces.web.port

import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.tag.TagName

interface UpdatePostUseCase {
    fun handle(
        postId: PostId,
        title: PostTitle,
        body: PostBody,
        tagNames: List<TagName>,
    )
}
