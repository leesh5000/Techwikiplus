package me.helloc.techwikiplus.common.infrastructure.persistence.jpa.mapper

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.PostEntity
import me.helloc.techwikiplus.post.domain.model.Post
import me.helloc.techwikiplus.post.domain.model.PostBody
import me.helloc.techwikiplus.post.domain.model.PostId
import me.helloc.techwikiplus.post.domain.model.PostStatus
import me.helloc.techwikiplus.post.domain.model.PostTitle
import org.springframework.stereotype.Component

@Component
class PostEntityMapper {
    fun toDomain(entity: PostEntity): Post {
        return Post(
            id = PostId(entity.id),
            title = PostTitle(entity.title),
            body = PostBody(entity.body),
            status = PostStatus.valueOf(entity.status),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
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
