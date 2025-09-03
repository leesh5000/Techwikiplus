package me.helloc.techwikiplus.post.domain.service

import me.helloc.techwikiplus.common.domain.service.port.ClockHolder
import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.tag.PostTag
import me.helloc.techwikiplus.post.domain.model.tag.Tag
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import me.helloc.techwikiplus.post.domain.service.port.PostIdGenerator
import me.helloc.techwikiplus.post.domain.service.port.PostRepository
import org.springframework.stereotype.Service

@Service
class PostWriteService(
    private val clockHolder: ClockHolder,
    private val postIdGenerator: PostIdGenerator,
    private val repository: PostRepository,
) {
    fun insert(
        title: PostTitle,
        body: PostBody,
        tagNames: List<TagName> = emptyList(),
    ): Post {
        val now = clockHolder.now()
        val postTags =
            tagNames.mapIndexed { index, tagName ->
                PostTag(tagName, index)
            }.toSet()
        val post =
            Post.create(
                id = postIdGenerator.next(),
                title = title,
                body = body,
                postTags = postTags,
                createdAt = now,
                updatedAt = now,
            )

        return repository.save(post)
    }

    fun update(
        post: Post,
        title: PostTitle,
        body: PostBody,
        tags: Set<Tag> = emptySet(),
    ): Post {
        val now = clockHolder.now()
        val postTags =
            tags.mapIndexed { index, tag ->
                PostTag(tag.name, index)
            }.toSet()
        val updatedPost =
            post.copy(
                title = title,
                body = body,
                tags = postTags,
                updatedAt = now,
            )
        return repository.save(updatedPost)
    }

    fun deleteSoft(post: Post): Post {
        val now = clockHolder.now()
        val deletedPost = post.delete(now)
        return repository.save(deletedPost).let { deletedPost }
    }
}
