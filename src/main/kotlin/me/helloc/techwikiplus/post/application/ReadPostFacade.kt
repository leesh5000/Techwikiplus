package me.helloc.techwikiplus.post.application

import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.service.PostReadService
import me.helloc.techwikiplus.post.interfaces.web.port.ReadPostUseCase
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class ReadPostFacade(
    private val postReadService: PostReadService,
) : ReadPostUseCase {
    override fun handle(postId: PostId): Post {
        return postReadService.getBy(postId)
    }
}
