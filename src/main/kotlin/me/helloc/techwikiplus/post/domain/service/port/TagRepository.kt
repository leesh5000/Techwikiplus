package me.helloc.techwikiplus.post.domain.service.port

import me.helloc.techwikiplus.post.domain.model.tag.Tag
import me.helloc.techwikiplus.post.domain.model.tag.TagId
import me.helloc.techwikiplus.post.domain.model.tag.TagName

interface TagRepository {
    fun findBy(id: TagId): Tag?

    fun findBy(name: TagName): Tag?

    fun findAllBy(names: List<TagName>): List<Tag>

    fun save(tag: Tag): Tag
}
