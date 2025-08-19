package me.helloc.techwikiplus.post.application

import me.helloc.techwikiplus.post.domain.model.PostBody
import me.helloc.techwikiplus.post.domain.model.PostId
import me.helloc.techwikiplus.post.domain.model.PostTitle
import me.helloc.techwikiplus.post.domain.service.PostRegister
import me.helloc.techwikiplus.post.interfaces.web.port.CreatePostUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class CreatePostFacade(
    private val postRegister: PostRegister,
) : CreatePostUseCase {
    override fun handle(
        title: PostTitle,
        body: PostBody,
    ): PostId {
        val post =
            postRegister.insert(
                title = title,
                body = body,
            )
        return post.id
    }
}
