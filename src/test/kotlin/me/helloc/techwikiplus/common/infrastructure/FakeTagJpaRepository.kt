package me.helloc.techwikiplus.common.infrastructure

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.TagJpaRepository
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.TagEntity
import org.springframework.data.domain.Example
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.query.FluentQuery
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function

class FakeTagJpaRepository : TagJpaRepository {
    private val tags = ConcurrentHashMap<Long, TagEntity>()
    private val postCounts = ConcurrentHashMap<Long, AtomicInteger>()

    override fun findByName(name: String): Optional<TagEntity> {
        return Optional.ofNullable(
            tags.values.firstOrNull { it.name == name },
        )
    }

    override fun findByNameIn(names: List<String>): List<TagEntity> {
        return tags.values.filter { it.name in names }
    }

    override fun incrementPostCount(tagId: Long) {
        val count = postCounts.computeIfAbsent(tagId) { AtomicInteger(0) }
        count.incrementAndGet()
    }

    override fun decrementPostCount(tagId: Long) {
        val count = postCounts.computeIfAbsent(tagId) { AtomicInteger(0) }
        val currentValue = count.get()
        if (currentValue > 0) {
            count.decrementAndGet()
        }
    }

    fun getPostCount(tagId: Long): Int {
        return postCounts[tagId]?.get() ?: 0
    }

    fun addTag(tagEntity: TagEntity) {
        tags[tagEntity.id] = tagEntity
        postCounts[tagEntity.id] = AtomicInteger(tagEntity.postCount)
    }

    fun addTagFromDomain(tag: me.helloc.techwikiplus.post.domain.model.tag.Tag) {
        val tagEntity =
            TagEntity(
                id = tag.id.value,
                name = tag.name.value,
                postCount = tag.postCount,
                createdAt = tag.createdAt,
                updatedAt = tag.updatedAt,
            )
        tags[tagEntity.id] = tagEntity
        postCounts[tagEntity.id] = AtomicInteger(tagEntity.postCount)
    }

    fun clear() {
        tags.clear()
        postCounts.clear()
    }

    // JpaRepository default methods implementation
    override fun <S : TagEntity> save(entity: S): S {
        tags[entity.id] = entity
        postCounts.computeIfAbsent(entity.id) { AtomicInteger(entity.postCount) }
        return entity
    }

    override fun <S : TagEntity> saveAll(entities: MutableIterable<S>): MutableList<S> {
        val result = mutableListOf<S>()
        entities.forEach { entity ->
            save(entity)
            result.add(entity)
        }
        return result
    }

    override fun findById(id: Long): Optional<TagEntity> {
        return Optional.ofNullable(tags[id])
    }

    override fun existsById(id: Long): Boolean {
        return tags.containsKey(id)
    }

    override fun findAll(): MutableList<TagEntity> {
        return tags.values.toMutableList()
    }

    override fun findAll(sort: Sort): MutableList<TagEntity> {
        return tags.values.toMutableList()
    }

    override fun findAllById(ids: MutableIterable<Long>): MutableList<TagEntity> {
        return ids.mapNotNull { tags[it] }.toMutableList()
    }

    override fun count(): Long {
        return tags.size.toLong()
    }

    override fun deleteById(id: Long) {
        tags.remove(id)
        postCounts.remove(id)
    }

    override fun delete(entity: TagEntity) {
        deleteById(entity.id)
    }

    override fun deleteAllById(ids: MutableIterable<Long>) {
        ids.forEach { deleteById(it) }
    }

    override fun deleteAll(entities: MutableIterable<TagEntity>) {
        entities.forEach { delete(it) }
    }

    override fun deleteAll() {
        clear()
    }

    override fun <S : TagEntity> findAll(example: Example<S>): MutableList<S> {
        throw UnsupportedOperationException("Not implemented in fake")
    }

    override fun <S : TagEntity> findAll(
        example: Example<S>,
        sort: Sort,
    ): MutableList<S> {
        throw UnsupportedOperationException("Not implemented in fake")
    }

    override fun <S : TagEntity> saveAllAndFlush(entities: MutableIterable<S>): MutableList<S> {
        return saveAll(entities)
    }

    override fun <S : TagEntity> saveAndFlush(entity: S): S {
        return save(entity)
    }

    override fun flush() {
        // No-op in fake implementation
    }

    override fun deleteAllInBatch(entities: MutableIterable<TagEntity>) {
        deleteAll(entities)
    }

    override fun deleteAllByIdInBatch(ids: MutableIterable<Long>) {
        deleteAllById(ids)
    }

    override fun deleteAllInBatch() {
        deleteAll()
    }

    override fun getOne(id: Long): TagEntity {
        return tags[id] ?: throw NoSuchElementException("Tag with id $id not found")
    }

    override fun getById(id: Long): TagEntity {
        return getOne(id)
    }

    override fun getReferenceById(id: Long): TagEntity {
        return getById(id)
    }

    override fun <S : TagEntity> exists(example: Example<S>): Boolean {
        throw UnsupportedOperationException("Not implemented in fake")
    }

    override fun <S : TagEntity, R : Any> findBy(
        example: Example<S>,
        queryFunction: Function<FluentQuery.FetchableFluentQuery<S>, R>,
    ): R {
        throw UnsupportedOperationException("Not implemented in fake")
    }

    override fun <S : TagEntity> count(example: Example<S>): Long {
        throw UnsupportedOperationException("Not implemented in fake")
    }

    override fun <S : TagEntity> findOne(example: Example<S>): Optional<S> {
        throw UnsupportedOperationException("Not implemented in fake")
    }

    override fun findAll(pageable: Pageable): Page<TagEntity> {
        throw UnsupportedOperationException("Not implemented in fake")
    }

    override fun <S : TagEntity> findAll(
        example: Example<S>,
        pageable: Pageable,
    ): Page<S> {
        throw UnsupportedOperationException("Not implemented in fake")
    }
}
