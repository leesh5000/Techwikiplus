package me.helloc.techwikiplus.common.infrastructure.persistence

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.TagJpaRepository
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.mapper.TagEntityMapper
import me.helloc.techwikiplus.post.domain.model.tag.Tag
import me.helloc.techwikiplus.post.domain.model.tag.TagId
import me.helloc.techwikiplus.post.domain.model.tag.TagName
import me.helloc.techwikiplus.post.domain.service.port.TagRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class TagRepositoryImpl(
    private val jpaRepository: TagJpaRepository,
    private val mapper: TagEntityMapper,
) : TagRepository {
    override fun findBy(id: TagId): Tag? {
        return jpaRepository.findById(id.value)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }

    override fun findBy(name: TagName): Tag? {
        return jpaRepository.findByName(name.value)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }

    override fun findAllBy(names: List<TagName>): List<Tag> {
        if (names.isEmpty()) return emptyList()

        val nameValues = names.map { it.value }
        return jpaRepository.findByNameIn(nameValues)
            .map { mapper.toDomain(it) }
    }

    @Transactional
    override fun save(tag: Tag): Tag {
        val entity = mapper.toEntity(tag)
        val savedEntity = jpaRepository.save(entity)
        return mapper.toDomain(savedEntity)
    }
}
