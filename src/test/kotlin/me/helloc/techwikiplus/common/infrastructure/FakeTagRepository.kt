package me.helloc.techwikiplus.common.infrastructure

import me.helloc.techwikiplus.post.domain.model.tag.Tag
import me.helloc.techwikiplus.post.domain.model.tag.TagId
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import me.helloc.techwikiplus.post.domain.service.port.TagRepository
import java.util.concurrent.ConcurrentHashMap

class FakeTagRepository : TagRepository {
    private val tags = ConcurrentHashMap<TagId, Tag>()
    private val tagsByName = ConcurrentHashMap<String, Tag>()

    override fun findBy(id: TagId): Tag? {
        return tags[id]
    }

    override fun findBy(name: TagName): Tag? {
        return tagsByName[name.value]
    }

    override fun findAllBy(names: List<TagName>): List<Tag> {
        return names.mapNotNull { tagsByName[it.value] }
    }

    override fun save(tag: Tag): Tag {
        // 원자적으로 두 맵을 업데이트
        synchronized(this) {
            tags[tag.id] = tag
            tagsByName[tag.name.value] = tag
        }
        return tag
    }

    fun findById(id: TagId): Tag? {
        return tags[id]
    }

    fun clear() {
        synchronized(this) {
            tags.clear()
            tagsByName.clear()
        }
    }

    fun getAll(): List<Tag> {
        return tags.values.toList()
    }

    fun findAll(): List<Tag> {
        return tags.values.toList()
    }

    fun count(): Int {
        return tags.size
    }
}
