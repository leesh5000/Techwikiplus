package me.helloc.techwikiplus.common.infrastructure.persistence.jpa.mapper

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.PostEntity
import me.helloc.techwikiplus.post.domain.model.post.Post
import me.helloc.techwikiplus.post.domain.model.post.PostBody
import me.helloc.techwikiplus.post.domain.model.post.PostId
import me.helloc.techwikiplus.post.domain.model.post.PostStatus
import me.helloc.techwikiplus.post.domain.model.post.PostTitle
import me.helloc.techwikiplus.post.domain.model.tag.PostTag
import org.springframework.stereotype.Component

@Component
class PostEntityMapper {
    fun toDomain(
        entity: PostEntity,
        tags: List<PostTag> = emptyList(),
    ): Post {
        return Post(
            id = PostId(entity.id),
            title = PostTitle(entity.title),
            body = PostBody(entity.body),
            status = PostStatus.valueOf(entity.status),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            tags = tags,
        )
    }

    fun toEntity(post: Post): PostEntity {
        return PostEntity(
            id = post.id.value,
            title = post.title.value,
            body = post.body.value,
            status = post.status.name,
            createdAt = post.createdAt,
            updatedAt = post.updatedAt,
        )
    }
}
