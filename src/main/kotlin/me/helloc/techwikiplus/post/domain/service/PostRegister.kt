package me.helloc.techwikiplus.post.domain.service

import me.helloc.techwikiplus.common.domain.service.port.ClockHolder
import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.service.port.PostIdGenerator
import me.helloc.techwikiplus.post.domain.service.port.PostRepository
import org.springframework.stereotype.Service

@Service
class PostRegister(
    private val clockHolder: ClockHolder,
    private val postIdGenerator: PostIdGenerator,
    private val repository: PostRepository,
) {
    fun insert(
        title: PostTitle,
        body: PostBody,
    ): Post {
        val now = clockHolder.now()
        val post =
            Post.create(
                id = postIdGenerator.next(),
                title = title,
                body = body,
                createdAt = now,
            )

        return repository.save(post)
    }
}
