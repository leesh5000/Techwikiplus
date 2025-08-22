package me.helloc.techwikiplus.post.domain.service.port

import me.helloc.techwikiplus.post.domain.model.tag.Tag
import me.helloc.techwikiplus.post.domain.model.tag.TagId
import me.helloc.techwikiplus.post.domain.model.tag.TagName

interface TagRepository {
    fun findById(id: TagId): Tag?

    fun findByName(name: TagName): Tag?

    fun findByNames(names: List<TagName>): List<Tag>

    fun findOrCreateByName(name: TagName): Tag

    fun findPopularTags(limit: Int): List<Tag>

    fun findByNamePrefix(
        prefix: String,
        limit: Int,
    ): List<Tag>

    fun save(tag: Tag): Tag

    fun incrementPostCount(tagId: TagId)

    fun decrementPostCount(tagId: TagId)
}
