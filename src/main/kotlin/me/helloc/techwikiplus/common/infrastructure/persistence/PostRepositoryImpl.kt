package me.helloc.techwikiplus.common.infrastructure.persistence

import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.PostJpaRepository
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.entity.PostEntity
import me.helloc.techwikiplus.common.infrastructure.persistence.jpa.mapper.PostEntityMapper
import me.helloc.techwikiplus.post.domain.model.Post
import me.helloc.techwikiplus.post.domain.model.PostId
import me.helloc.techwikiplus.post.domain.service.port.PostRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class PostRepositoryImpl(
    private val jpaRepository: PostJpaRepository,
    private val mapper: PostEntityMapper,
) : PostRepository {
    override fun findBy(id: PostId): Post? {
        return jpaRepository.findById(id.value)
            .orElse(null)
            ?.let {
                mapper.toDomain(it)
            }
    }

    override fun existsBy(id: PostId): Boolean {
        return jpaRepository.existsById(id.value)
    }

    @Transactional
    override fun save(post: Post): Post {
        val entity = mapper.toEntity(post)
        val savedEntity = jpaRepository.save<PostEntity>(entity)
        return mapper.toDomain(savedEntity)
    }
}
