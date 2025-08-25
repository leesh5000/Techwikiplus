package me.helloc.techwikiplus.common.infrastructure.persistence.jpa.mapper

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.TagEntity
import me.helloc.techwikiplus.post.domain.model.tag.Tag
import me.helloc.techwikiplus.post.domain.model.tag.TagId
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import org.springframework.stereotype.Component

@Component
class TagEntityMapper {
    fun toDomain(entity: TagEntity): Tag {
        return Tag(
            id = TagId(entity.id),
            name = TagName(entity.name),
            postCount = entity.postCount,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    fun toEntity(tag: Tag): TagEntity {
        return TagEntity(
            id = tag.id.value,
            name = tag.name.value,
            postCount = tag.postCount,
            createdAt = tag.createdAt,
            updatedAt = tag.updatedAt,
        )
    }
}
