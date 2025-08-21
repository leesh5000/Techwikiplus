package me.helloc.techwikiplus.post.application

import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.service.PostAuthorizationService
import me.helloc.techwikiplus.post.domain.service.PostRegister
import me.helloc.techwikiplus.post.interfaces.web.port.CreatePostUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class CreatePostFacade(
    private val postRegister: PostRegister,
    private val postAuthorizationService: PostAuthorizationService,
) : CreatePostUseCase {
    override fun handle(
        title: PostTitle,
        body: PostBody,
    ): PostId {
        postAuthorizationService.requireAdminRole()

        val post =
            postRegister.insert(
                title = title,
                body = body,
            )
        return post.id
    }
}
