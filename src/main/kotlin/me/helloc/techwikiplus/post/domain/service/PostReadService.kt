package me.helloc.techwikiplus.post.domain.service

import me.helloc.techwikiplus.post.domain.exception.PostDomainException
import me.helloc.techwikiplus.post.domain.exception.PostErrorCode
import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.service.port.PostRepository
import org.springframework.stereotype.Service

@Service
class PostReadService(
    private val repository: PostRepository,
) {
    fun getBy(postId: PostId): Post {
        return repository.findBy(postId)
            ?: throw PostDomainException(PostErrorCode.POST_NOT_FOUND, arrayOf(postId))
    }
}
